package net.hardnorth.github.merge.service;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import net.hardnorth.github.merge.exception.ConnectionException;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.utils.WebClientCommon;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import retrofit2.Response;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class GithubOAuthService {
    public static final String DEFAULT_GITHUB_OAUTH_URL = "https://github.com/login/oauth/authorize";
    public static final List<String> SCOPES = Arrays.asList("repo", "user:email");

    public static final String REDIRECT_URI_PATTERN = "%s/integration/result/%s";
    private static final String AUTHORIZATION_KIND = "github-oauth";
    private static final String AUTH_UUID = "authUuid";
    private static final String EXPIRES = "expires";
    private static final String STATE = "state";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String ACCESS_TOKEN_TYPE = "token_type";

    private final Datastore datastore;
    private final GithubClient github;
    private final KeyFactory authKeyFactory;

    private final String baseUrl;
    private final GithubCredentials credentials;
    private String githubOAuthUrl = DEFAULT_GITHUB_OAUTH_URL;


    @SuppressWarnings("CdiInjectionPointsInspection")
    public GithubOAuthService(Datastore datastoreService, GithubClient githubApi, String serviceUrl,
                              GithubCredentials githubCredentials) {
        datastore = datastoreService;
        github = githubApi;
        baseUrl = serviceUrl;
        authKeyFactory = datastore.newKeyFactory().setKind(AUTHORIZATION_KIND);
        credentials = githubCredentials;
    }

    public String authenticate(String authUuid) {
        return null;
    }

    public String createIntegration() {
        String authUuid = UUID.randomUUID().toString();
        String stateUuid = UUID.randomUUID().toString();
        Key authKey = datastore.allocateId(authKeyFactory.newKey());
        Calendar expireTime = Calendar.getInstance();
        expireTime.add(Calendar.HOUR, 1);
        Entity auth = Entity.newBuilder(authKey)
                .set(AUTH_UUID, authUuid)
                .set(EXPIRES, Timestamp.of(expireTime.getTime()))
                .set(STATE, stateUuid)
                .build();
        datastore.put(auth);
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(githubOAuthUrl);
            uriBuilder.addParameter("redirect_uri", String.format(REDIRECT_URI_PATTERN, baseUrl, authUuid));
            uriBuilder.addParameter("client_id", credentials.getId());
            uriBuilder.addParameter("scope", StringUtils.joinWith(" ", SCOPES.toArray()));
            uriBuilder.addParameter(STATE, stateUuid);
            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void authorize(String authUuid, String code, String state) {
        EntityQuery query = Query.newEntityQueryBuilder()
                .setKind(AUTHORIZATION_KIND)
                .setFilter(
                        StructuredQuery.CompositeFilter.and(
                                StructuredQuery.PropertyFilter.eq(AUTH_UUID, authUuid),
                                StructuredQuery.PropertyFilter.le(EXPIRES, Timestamp.now())
                        )
                ).build();
        QueryResults<Entity> result = datastore.run(query);
        if (!result.hasNext() || !state.equals(result.next().getString(STATE))) {
            throw new UnauthorizedException("Unable to validate your request: invalid authentication UUID ar state, or your authorization already expired");
        }

        Response<JsonObject> rs = WebClientCommon.executeServiceCall(github.loginApplication(credentials.getId(), credentials.getToken(), code, state, null));
        if(!rs.isSuccessful() || rs.body() == null) {
            throw new ConnectionException("Unable to connect to Github API");
        }
        JsonObject body = rs.body();
        if(!body.has(ACCESS_TOKEN) || !body.has(ACCESS_TOKEN_TYPE)) {
            throw new ConnectionException("Invalid response from Github API");
        }
        JsonElement tokenObject = body.get(ACCESS_TOKEN);
        JsonElement tokenTypeObject = body.get(ACCESS_TOKEN_TYPE);
        if(tokenObject.isJsonNull() || tokenTypeObject.isJsonNull()) {
            throw new ConnectionException("Invalid response from Github API");
        }
        String token = tokenObject.getAsString();
        String tokenType = tokenTypeObject.getAsString();
        String authenticationStr = tokenType + " " + token;

    }

    public void setGithubOAuthUrl(String githubOAuthUrl) {
        this.githubOAuthUrl = githubOAuthUrl;
    }
}
