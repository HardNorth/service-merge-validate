package net.hardnorth.github.merge.service;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.impl.GithubOAuthService;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GithubOAuthServiceTest {
    public static final String PROJECT_ID = "test";
    public static final int SERVICE_PORT = 8889;
    public static final String SERVICE_URL = "http://localhost:" + SERVICE_PORT;
    public static final String GITHUB_URL = "https://github.com/login/oauth/authorize";
    public static final String APPLICATION_NAME = "test-application";
    public static final String CLIENT_ID = "test-client-id";
    public static final String CLIENT_SECRET = "test-client-secret";

    private final GithubClient github = mock(GithubClient.class);
    private final EncryptionService encryptionService = mock(EncryptionService.class);

    private final Datastore datastore = DatastoreOptions.newBuilder().setNamespace(APPLICATION_NAME).build().getService();
    private final GithubOAuthService service = new GithubOAuthService(datastore, github, encryptionService, SERVICE_URL,
            new GithubCredentials(CLIENT_ID, CLIENT_SECRET));

    private static Map<String, String> parseQuery(String url) throws MalformedURLException {
        return Arrays.stream(new URL(url).getQuery().split("&"))
                .collect(Collectors.toMap(s -> s.split("=")[0], s -> URLDecoder.decode(s.split("=")[1], StandardCharsets.UTF_8)));
    }

    @Test
    public void verify_createIntegration_url() throws MalformedURLException {
        String urlStr = service.createIntegration();
        assertThat(urlStr, startsWith(GITHUB_URL));

        Map<String, String> query =parseQuery(urlStr);

        assertThat(query, hasEntry(equalTo("redirect_uri"), startsWith(SERVICE_URL + "/integration/result/")));
        assertThat(query, hasEntry(equalTo("state"), not(emptyOrNullString())));
        assertThat(query, hasEntry(equalTo("client_id"), equalTo(CLIENT_ID)));
        assertThat(query, hasEntry(equalTo("scope"), equalTo("repo user:email")));
    }

    @Test
    public void verify_createIntegration_database_entry_creation() throws MalformedURLException {
        String urlStr = service.createIntegration();
        Map<String, String> urlQuery = parseQuery(urlStr);

        String authToken = urlQuery.get("redirect_uri").substring(urlQuery.get("redirect_uri").lastIndexOf('/') + 1);
        byte[] authTokenBytes = Base64.getUrlDecoder().decode(authToken);
        byte[] keyBytes = new byte[authTokenBytes[1]];
        System.arraycopy(authTokenBytes, 2, keyBytes, 0, keyBytes.length);
        long keyValue = new BigInteger(keyBytes).longValue();
        Key key = datastore.newKeyFactory().setKind("github-oauth").newKey(keyValue);

        Entity entity = datastore.get(key);
        assertThat(entity, notNullValue());
        Calendar futureCalendar = Calendar.getInstance();
        futureCalendar.add(Calendar.HOUR, 1);
        assertThat(
                entity.getTimestamp("expires").toDate(),
                allOf(greaterThan(Calendar.getInstance().getTime()), lessThanOrEqualTo(futureCalendar.getTime()))
        );
    }

    @Test
    public void verify_authorization_success() throws IOException {
        String urlStr = service.createIntegration();
        Map<String, String> urlQuery = parseQuery(urlStr);
        String authToken = urlStr.substring(urlStr.lastIndexOf('/') + 1);
        String code = UUID.randomUUID().toString();
        Call<JsonObject> call = mock(Call.class);
        Response<JsonObject> response = mock(Response.class);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(Boolean.TRUE);

        String accessToken = UUID.randomUUID().toString();
        String tokenType = "bearer";
        JsonObject responseBody = new JsonObject();
        responseBody.addProperty("access_token", accessToken);
        responseBody.addProperty("token_type", tokenType);
        when(response.body()).thenReturn(responseBody);

        when(github.loginApplication(eq(CLIENT_ID), eq(CLIENT_SECRET), same(code), eq(urlQuery.get("state")), isNull())).thenReturn(call);
//        service.authorize()
    }
}
