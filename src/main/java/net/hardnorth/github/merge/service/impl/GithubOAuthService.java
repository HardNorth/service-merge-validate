package net.hardnorth.github.merge.service.impl;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import net.hardnorth.github.merge.exception.ConnectionException;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.EncryptedStorage;
import net.hardnorth.github.merge.service.GithubClient;
import net.hardnorth.github.merge.service.OAuthService;
import net.hardnorth.github.merge.utils.KeyType;
import net.hardnorth.github.merge.utils.Keys;
import net.hardnorth.github.merge.utils.WebClientCommon;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import retrofit2.Response;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static net.hardnorth.github.merge.utils.Keys.*;

public class GithubOAuthService implements OAuthService {

    public static final RuntimeException AUTHORIZATION_EXCEPTION = new UnauthorizedException("Unable to validate your request: invalid authentication token or state, or your authorization already expired");
    public static final RuntimeException AUTHENTICATION_EXCEPTION = new AuthenticationFailedException("Invalid authentication token");
    public static final RuntimeException INVALID_API_RESPONSE = new ConnectionException("Invalid response from Github API");
    public static final String DEFAULT_GITHUB_OAUTH_URL = "https://github.com/login/oauth/authorize";
    public static final List<String> SCOPES = Arrays.asList("repo", "user:email");

    public static final String REDIRECT_URI_PATTERN = "%s/integration/result/%s";
    private static final String AUTHORIZATION_KIND = "github-oauth";
    private static final String AUTHENTICATION_KIND = "authentication";
    private static final String AUTH_HASH = "authHash";
    private static final String EXPIRES = "expires";
    private static final String STATE = "state";
    private static final String DATA_REFERENCE = "dataRef";
    private static final String CREATION_DATE = "creationDate";
    private static final String ACCESS_DATE = "accessDate";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String ACCESS_TOKEN_TYPE = "token_type";

    private final Datastore datastore;
    private final GithubClient github;
    private final EncryptedStorage storage;
    private final KeyFactory authorizationKeyFactory;
    private final KeyFactory authenticationKeyFactory;

    private final String baseUrl;
    private final GithubCredentials credentials;
    private String githubOAuthUrl = DEFAULT_GITHUB_OAUTH_URL;


    @SuppressWarnings("CdiInjectionPointsInspection")
    public GithubOAuthService(Datastore datastoreService, GithubClient githubApi, EncryptedStorage encryptedStorage,
                              String serviceUrl, GithubCredentials githubCredentials) {
        datastore = datastoreService;
        github = githubApi;
        baseUrl = serviceUrl;
        authorizationKeyFactory = datastore.newKeyFactory().setKind(AUTHORIZATION_KIND);
        authenticationKeyFactory = datastore.newKeyFactory().setKind(AUTHENTICATION_KIND);
        credentials = githubCredentials;
        storage = encryptedStorage;
    }

    @Override
    @Nonnull
    public String authenticate(@Nonnull String authToken) {
        Triple<KeyType, byte[], byte[]> bareToken = decodeAuthToken(authToken);
        Key authKey = bareToken.getLeft() == KeyType.LONG ?
                authenticationKeyFactory.newKey(new BigInteger(bareToken.getMiddle()).longValue()) :
                authenticationKeyFactory.newKey(new String(bareToken.getMiddle(), StandardCharsets.UTF_8));
        Entity entity = datastore.get(authKey);
        if (entity == null || !BCrypt.checkpw(bareToken.getRight(), entity.getString(AUTH_HASH))) {
            throw AUTHENTICATION_EXCEPTION;
        }

        String githubToken = storage.getValue(entity.getString(DATA_REFERENCE), Hex.encodeHexString(bareToken.getRight()));
        if (githubToken == null) {
            throw new IllegalArgumentException("Unable to find GitHub data");
        }
        return githubToken;
    }

    private Pair<byte[], String> generateAuthToken() {
        UUID authUuid = UUID.randomUUID();
        byte[] authUuidBytes = getBytes(authUuid);
        String salt = BCrypt.gensalt(12);
        String authUuidHash = BCrypt.hashpw(authUuidBytes, salt);
        return Pair.of(authUuidBytes, authUuidHash);
    }

    private String generateTokenString(Key authKey, byte[] tokenBytes) {
        Object bareKey = authKey.getNameOrId();
        KeyType type = KeyType.getKeyType(bareKey);
        byte[] keyBytes = Keys.getKeyBytes(bareKey);
        return encodeAuthToken(type, keyBytes, tokenBytes);
    }

    @Override
    @Nonnull
    public String createIntegration() {
        Pair<byte[], String> authToken = generateAuthToken();
        Key authKey = datastore.allocateId(authorizationKeyFactory.newKey());
        String authTokenStr = generateTokenString(authKey, authToken.getLeft());

        Calendar expireTime = Calendar.getInstance();
        expireTime.add(Calendar.HOUR, 1);
        String stateUuid = UUID.randomUUID().toString();
        Entity auth = Entity.newBuilder(authKey)
                .set(AUTH_HASH, authToken.getRight())
                .set(EXPIRES, Timestamp.of(expireTime.getTime()))
                .set(STATE, stateUuid)
                .build();
        datastore.add(auth);
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(githubOAuthUrl);
            uriBuilder.addParameter("redirect_uri", String.format(REDIRECT_URI_PATTERN, baseUrl, authTokenStr));
            uriBuilder.addParameter("client_id", credentials.getId());
            uriBuilder.addParameter("scope", StringUtils.joinWith(" ", SCOPES.toArray()));
            uriBuilder.addParameter(STATE, stateUuid);
            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @Nonnull
    public String authorize(@Nonnull String authToken, @Nonnull String code, @Nonnull String state) {
        // Authenticate
        Triple<KeyType, byte[], byte[]> bareToken = decodeAuthToken(authToken);
        Key authKey = bareToken.getLeft() == KeyType.LONG ?
                authorizationKeyFactory.newKey(new BigInteger(bareToken.getMiddle()).longValue()) :
                authorizationKeyFactory.newKey(new String(bareToken.getMiddle(), StandardCharsets.UTF_8));

        Timestamp now = Timestamp.now();
        Entity entity = datastore.get(authKey);
        if (entity == null || !BCrypt.checkpw(bareToken.getRight(), entity.getString(AUTH_HASH)) ||
                !entity.getString(STATE).equals(state) || now.compareTo(entity.getTimestamp(EXPIRES)) > 0) {
            throw AUTHORIZATION_EXCEPTION;
        } else {
            // Cleanup temporary authorization data
            datastore.delete(authKey);
        }

        // Authorize
        Response<JsonObject> rs = WebClientCommon.executeServiceCall(github.loginApplication(credentials.getId(),
                credentials.getToken(), code, state, null));
        if (!rs.isSuccessful() || rs.body() == null) {
            throw new ConnectionException("Unable to connect to Github API");
        }
        JsonObject body = rs.body();
        if (!body.has(ACCESS_TOKEN) || !body.has(ACCESS_TOKEN_TYPE)) {
            throw INVALID_API_RESPONSE;
        }
        JsonElement tokenObject = body.get(ACCESS_TOKEN);
        JsonElement tokenTypeObject = body.get(ACCESS_TOKEN_TYPE);
        if (tokenObject.isJsonNull() || tokenTypeObject.isJsonNull()) {
            throw INVALID_API_RESPONSE;
        }
        String githubToken = tokenObject.getAsString();
        String tokenType = tokenTypeObject.getAsString();
        String githubAuthenticationStr = tokenType + " " + githubToken;

        // Generate new authentication data
        Pair<byte[], String> userToken = generateAuthToken();
        Key userAuthKey = datastore.allocateId(authenticationKeyFactory.newKey());
        String userTokenStr = generateTokenString(userAuthKey, userToken.getLeft());
        String credentialsKey = UUID.randomUUID().toString();

        Entity userAuth = Entity.newBuilder(userAuthKey)
                .set(AUTH_HASH, userToken.getRight())
                .set(CREATION_DATE, now)
                .set(ACCESS_DATE, now)
                .set(DATA_REFERENCE, credentialsKey)
                .build();
        datastore.add(userAuth);
        storage.saveValue(credentialsKey, githubAuthenticationStr, Hex.encodeHexString(userToken.getLeft()));
        return userTokenStr;
    }

    public void setGithubOAuthUrl(String githubOAuthUrl) {
        this.githubOAuthUrl = githubOAuthUrl;
    }
}
