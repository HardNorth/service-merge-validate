package net.hardnorth.github.merge.service;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.JsonObject;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.impl.GithubOAuthService;
import net.hardnorth.github.merge.utils.KeyType;
import net.hardnorth.github.merge.utils.Keys;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

        Map<String, String> query = parseQuery(urlStr);

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
        String redirectUriPath = new URL(urlQuery.get("redirect_uri")).getPath();
        String authToken = redirectUriPath.substring(redirectUriPath.lastIndexOf('/') + 1);
        String state = urlQuery.get("state");
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
        when(encryptionService.encrypt(any(), any())).thenReturn(new byte[]{0, 0, 0});

        when(github.loginApplication(eq(CLIENT_ID), eq(CLIENT_SECRET), same(code), eq(state), isNull())).thenReturn(call);
        String result = service.authorize(authToken, code, state);
        assertThat(result, not(emptyOrNullString()));

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(encryptionService).encrypt(captor.capture(), any());
        List<byte[]> encryptionValues = captor.getAllValues();
        assertThat(encryptionValues.get(0), equalTo((tokenType + " " + accessToken).getBytes(StandardCharsets.UTF_8)));

        Triple<KeyType, byte[], byte[]> resultToken = Keys.decodeAuthToken(result);
        Key key = datastore.newKeyFactory().setKind("integrations").newKey(new BigInteger(resultToken.getMiddle()).longValue());
        Entity entity = datastore.get(key);
        assertThat(entity, notNullValue());
        Date now = Calendar.getInstance().getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -1);
        Date minuteAgo = calendar.getTime();

        assertThat(
                entity.getTimestamp("creationDate").toDate(),
                allOf(lessThanOrEqualTo(now), greaterThan(minuteAgo))
        );

        assertThat(
                entity.getTimestamp("accessDate").toDate(),
                allOf(lessThanOrEqualTo(now), greaterThan(minuteAgo))
        );

        assertThat(entity.getString("data"), equalTo("AAAA"));
    }
}
