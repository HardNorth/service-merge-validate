package net.hardnorth.github.merge.service;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class GithubOAuthService {
    public static final String DEFAULT_GITHUB_OAUTH_URL = "https://github.com/login/oauth/authorize";
    public static final List<String> SCOPES = Arrays.asList("repo", "user:email");

    public static final String REDIRECT_URI_PATTERN = "%s/integration/result/%s";
    private static final String AUTH_KIND = "github-oauth";
    private static final String STATE = "state";

    private final Datastore datastore;
    private final KeyFactory keyFactory;
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private String githubOAuthUrl = DEFAULT_GITHUB_OAUTH_URL;


    @SuppressWarnings("CdiInjectionPointsInspection")
    public GithubOAuthService(Datastore datastoreService, String applicationName, String serviceUrl,
                              String githubApplicationClientId, String githubApplicationClientSecret) {
        datastore = datastoreService;
        baseUrl = serviceUrl;
        keyFactory = datastore.newKeyFactory().setKind(applicationName + "-" + AUTH_KIND);
        clientId = githubApplicationClientId;
        clientSecret = githubApplicationClientSecret;
    }

    public String authenticate(String authUuid) {
        return null;
    }

    public String createIntegration() {
        String authUuid = UUID.randomUUID().toString();
        String stateUuid = UUID.randomUUID().toString();
        Key authKey = datastore.allocateId(keyFactory.newKey());
        Calendar expireTime = Calendar.getInstance();
        expireTime.add(Calendar.HOUR, 1);
        Entity auth = Entity.newBuilder(authKey)
                .set("authUuid", authUuid)
                .set("expires", Timestamp.of(expireTime.getTime()))
                .set(STATE, stateUuid)
                .build();
        datastore.put(auth);
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(githubOAuthUrl);
            uriBuilder.addParameter("redirect_uri", String.format(REDIRECT_URI_PATTERN, baseUrl, authUuid));
            uriBuilder.addParameter("client_id", clientId);
            uriBuilder.addParameter("scope", StringUtils.joinWith(" ", SCOPES.toArray()));
            uriBuilder.addParameter(STATE, stateUuid);
            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void authorize(String authUuid, String code, String state) {

    }

    public void setGithubOAuthUrl(String githubOAuthUrl) {
        this.githubOAuthUrl = githubOAuthUrl;
    }
}
