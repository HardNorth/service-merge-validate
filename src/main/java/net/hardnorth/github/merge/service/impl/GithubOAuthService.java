package net.hardnorth.github.merge.service.impl;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import io.quarkus.security.AuthenticationFailedException;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.model.Token;
import net.hardnorth.github.merge.service.EncryptionService;
import net.hardnorth.github.merge.service.Github;
import net.hardnorth.github.merge.service.OAuthService;
import net.hardnorth.github.merge.utils.KeyType;
import net.hardnorth.github.merge.utils.Keys;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.client.utils.URIBuilder;
import org.jboss.logging.Logger;
import org.springframework.security.crypto.bcrypt.BCrypt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.*;

import static net.hardnorth.github.merge.utils.Keys.decodeAuthToken;
import static net.hardnorth.github.merge.utils.Keys.encodeAuthToken;

public class GithubOAuthService implements OAuthService {

    private static final Logger LOGGER = Logger.getLogger(GithubOAuthService.class);

    public static final RuntimeException INVALID_AUTH_TOKEN =
            new IllegalArgumentException("Unable to validate your request: invalid authentication token or state, or your authorization already expired");
    public static final RuntimeException AUTHENTICATION_EXCEPTION =
            new AuthenticationFailedException("Invalid authentication token");

    public static final String DEFAULT_GITHUB_OAUTH_URL = "https://github.com/login/oauth/authorize";
    public static final List<String> SCOPES = Collections.singletonList("repo");

    public static final String REDIRECT_URI_PATTERN = "%s/integration/result?authUuid=%s";
    private static final String AUTHORIZATION_KIND = "github-oauth";
    private static final String INTEGRATIONS_KIND = "integrations";
    private static final String AUTH_HASH = "authHash";
    private static final String EXPIRES = "expires";
    private static final String STATE = "state";
    private static final String INTEGRATION_DATA = "data";
    private static final String CREATION_DATE = "creationDate";
    private static final String ACCESS_DATE = "accessDate";

    private final Datastore datastore;
    private final Github github;
    private final EncryptionService encryption;
    private final KeyFactory authorizationKeyFactory;
    private final KeyFactory integrationKeyFactory;
    private final java.nio.charset.Charset charset;

    private final String baseUrl;
    private final GithubCredentials credentials;
    private String githubOAuthUrl = DEFAULT_GITHUB_OAUTH_URL;


    @SuppressWarnings("CdiInjectionPointsInspection")
    public GithubOAuthService(Datastore datastoreService, Github githubApi, EncryptionService encryptedService,
                              String serviceUrl, GithubCredentials githubCredentials, Charset currentCharset) {
        datastore = datastoreService;
        github = githubApi;
        baseUrl = serviceUrl;
        authorizationKeyFactory = datastore.newKeyFactory().setKind(AUTHORIZATION_KIND);
        integrationKeyFactory = datastore.newKeyFactory().setKind(INTEGRATIONS_KIND);
        credentials = githubCredentials;
        encryption = encryptedService;
        charset = currentCharset.getValue();
    }

    @Override
    @Nonnull
    public String authenticate(@Nullable String authToken) {
        Triple<KeyType, byte[], Token> bareToken = decodeAuthToken(authToken);
        Key authKey = bareToken.getLeft() == KeyType.LONG ?
                integrationKeyFactory.newKey(new BigInteger(bareToken.getMiddle()).longValue()) :
                integrationKeyFactory.newKey(new String(bareToken.getMiddle(), charset));
        Entity entity = datastore.get(authKey);
        if (entity == null) {
            throw AUTHENTICATION_EXCEPTION;
        }
        String hash = entity.getString(AUTH_HASH);
        if (!BCrypt.checkpw(bareToken.getRight().getValue(), hash)) {
            throw AUTHENTICATION_EXCEPTION;
        }
        try {
            return new String(encryption.decrypt(Base64.getDecoder().decode(entity.getString(INTEGRATION_DATA)), bareToken.getRight().getValue()), charset);
        } finally {
            Entity valueAccess = Entity.newBuilder(entity)
                    .set(ACCESS_DATE, Timestamp.now())
                    .build();
            datastore.put(valueAccess);
        }
    }

    private Pair<Token, String> generateAuthToken() {
        Token token = new Token(UUID.randomUUID());
        String salt = BCrypt.gensalt(12);
        String authUuidHash = BCrypt.hashpw(token.getValue(), salt);
        return Pair.of(token, authUuidHash);
    }

    private String generateTokenString(Key authKey, byte[] tokenBytes) {
        Object bareKey = authKey.getNameOrId();
        KeyType type = KeyType.getKeyType(bareKey);
        byte[] keyBytes = Keys.getKeyBytes(bareKey, charset);
        return encodeAuthToken(type, keyBytes, tokenBytes);
    }

    @Override
    @Nonnull
    public String createIntegration() {
        Pair<Token, String> tokenHash = generateAuthToken();
        Key authKey = datastore.allocateId(authorizationKeyFactory.newKey());
        String authTokenStr = generateTokenString(authKey, tokenHash.getLeft().getValue());

        Calendar expireTime = Calendar.getInstance();
        expireTime.add(Calendar.HOUR, 1);
        String stateUuid = UUID.randomUUID().toString();
        Entity auth = Entity.newBuilder(authKey)
                .set(AUTH_HASH, tokenHash.getRight())
                .set(EXPIRES, Timestamp.of(expireTime.getTime()))
                .set(STATE, stateUuid)
                .build();
        datastore.add(auth);
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(githubOAuthUrl);
            uriBuilder.addParameter("scope", StringUtils.joinWith(" ", SCOPES.toArray()));
            uriBuilder.addParameter("redirect_uri", String.format(REDIRECT_URI_PATTERN, baseUrl, authTokenStr));
            uriBuilder.addParameter("client_id", credentials.getId());
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
        Triple<KeyType, byte[], Token> bareToken = decodeAuthToken(authToken);
        Key authKey = bareToken.getLeft() == KeyType.LONG ?
                authorizationKeyFactory.newKey(new BigInteger(bareToken.getMiddle()).longValue()) :
                authorizationKeyFactory.newKey(new String(bareToken.getMiddle(), charset));

        Timestamp now = Timestamp.now();
        Entity entity = datastore.get(authKey);
        boolean notFound, hashInvalid = false, stateInvalid = false, expired = false;
        if ((notFound = entity == null) || (hashInvalid = !BCrypt.checkpw(bareToken.getRight().getValue(), entity.getString(AUTH_HASH))) ||
                (stateInvalid = !entity.getString(STATE).equals(state)) || (expired = now.compareTo(entity.getTimestamp(EXPIRES)) > 0)) {
            LOGGER.warnf("Null entity: %s; Invalid hash: %s; State invalid: %s; Expired: %s", notFound, hashInvalid, stateInvalid, expired);
            throw INVALID_AUTH_TOKEN;
        } else {
            // Cleanup temporary authorization data
            datastore.delete(authKey);
        }
        String githubAuthenticationStr = github.loginApplication(code, state);

        // Generate new authentication data
        Pair<Token, String> userTokenHash = generateAuthToken();
        Key userAuthKey = datastore.allocateId(integrationKeyFactory.newKey());
        String userTokenStr = generateTokenString(userAuthKey, userTokenHash.getLeft().getValue());

        Entity integration = Entity.newBuilder(userAuthKey)
                .set(AUTH_HASH, userTokenHash.getRight())
                .set(CREATION_DATE, now)
                .set(ACCESS_DATE, now)
                .set(INTEGRATION_DATA, Base64.getEncoder().encodeToString(encryption.encrypt(githubAuthenticationStr.getBytes(charset), userTokenHash.getLeft().getValue())))
                .build();
        datastore.add(integration);
        return userTokenStr;
    }

    public void setGithubOAuthUrl(String githubOAuthUrl) {
        this.githubOAuthUrl = githubOAuthUrl;
    }
}
