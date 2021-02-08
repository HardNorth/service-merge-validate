package net.hardnorth.github.merge.service.impl;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.quarkus.security.UnauthorizedException;
import net.hardnorth.github.merge.exception.ConnectionException;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.EncryptedStorage;
import net.hardnorth.github.merge.service.GithubClient;
import net.hardnorth.github.merge.utils.KeyType;
import net.hardnorth.github.merge.utils.Keys;
import net.hardnorth.github.merge.utils.WebClientCommon;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import retrofit2.Response;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static net.hardnorth.github.merge.utils.Keys.*;

public class GithubOAuthService {

    public static final RuntimeException AUTHORIZATION_EXCEPTION = new UnauthorizedException("Unable to validate your request: invalid authentication token or state, or your authorization already expired");
    public static final RuntimeException INVALID_API_RESPONSE = new ConnectionException("Invalid response from Github API");
    public static final String DEFAULT_GITHUB_OAUTH_URL = "https://github.com/login/oauth/authorize";
    public static final List<String> SCOPES = Arrays.asList("repo", "user:email");

    public static final String REDIRECT_URI_PATTERN = "%s/integration/result/%s";
    private static final String AUTHORIZATION_KIND = "github-oauth";
    private static final String AUTH_HASH = "authHash";
    private static final String EXPIRES = "expires";
    private static final String STATE = "state";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String ACCESS_TOKEN_TYPE = "token_type";

    private final Datastore datastore;
    private final GithubClient github;
    private final EncryptedStorage storage;
    private final KeyFactory authKeyFactory;

    private final String baseUrl;
    private final GithubCredentials credentials;
    private String githubOAuthUrl = DEFAULT_GITHUB_OAUTH_URL;


    @SuppressWarnings("CdiInjectionPointsInspection")
    public GithubOAuthService(Datastore datastoreService, GithubClient githubApi, EncryptedStorage encryptedStorage,
                              String serviceUrl, GithubCredentials githubCredentials) {
        datastore = datastoreService;
        github = githubApi;
        baseUrl = serviceUrl;
        authKeyFactory = datastore.newKeyFactory().setKind(AUTHORIZATION_KIND);
        credentials = githubCredentials;
        storage = encryptedStorage;
    }

    public String authenticate(String authUuid) {
        return null;
    }

    public String createIntegration() {
        UUID authUuid = UUID.randomUUID();
        byte[] authUuidBytes = getBytes(authUuid);
        String salt = BCrypt.gensalt(12);
        String authUuidHash = BCrypt.hashpw(authUuidBytes, salt);

        Key authKey = datastore.allocateId(authKeyFactory.newKey());

        Object bareKey = authKey.getNameOrId();
        KeyType type = KeyType.getKeyType(bareKey);
        byte[] keyBytes = Keys.getKeyBytes(bareKey);
        String authTokenStr = getAuthToken(type, keyBytes, authUuidBytes);

        Calendar expireTime = Calendar.getInstance();
        expireTime.add(Calendar.HOUR, 1);
        String stateUuid = UUID.randomUUID().toString();
        Entity auth = Entity.newBuilder(authKey)
                .set(AUTH_HASH, authUuidHash)
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

    public void authorize(String authToken, String code, String state) {
        Triple<KeyType, byte[], byte[]> bareToken = decodeAuthToken(authToken);
        Key authKey = bareToken.getLeft() == KeyType.LONG ?
                authKeyFactory.newKey(new BigInteger(bareToken.getMiddle()).longValue()) :
                authKeyFactory.newKey(new String(bareToken.getMiddle(), StandardCharsets.UTF_8));

        Entity entity = datastore.get(authKey);
        if (entity == null || !BCrypt.checkpw(bareToken.getRight(), entity.getString(AUTH_HASH))) {
            throw AUTHORIZATION_EXCEPTION;
        }

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
        String token = tokenObject.getAsString();
        String tokenType = tokenTypeObject.getAsString();
        String authenticationStr = tokenType + " " + token;
        storage.saveValue(null, authToken, authenticationStr);
    }

    public void setGithubOAuthUrl(String githubOAuthUrl) {
        this.githubOAuthUrl = githubOAuthUrl;
    }
}
