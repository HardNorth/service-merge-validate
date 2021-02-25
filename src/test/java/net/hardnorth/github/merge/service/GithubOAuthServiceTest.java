package net.hardnorth.github.merge.service;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.JsonObject;
import io.quarkus.security.AuthenticationFailedException;
import net.hardnorth.github.merge.model.Charset;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.impl.GithubOAuthService;
import net.hardnorth.github.merge.utils.KeyType;
import net.hardnorth.github.merge.utils.Keys;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
    public static final int SERVICE_PORT = 8889;
    public static final String SERVICE_URL = "http://localhost:" + SERVICE_PORT;
    public static final String GITHUB_URL = "https://github.com/login/oauth/authorize";
    public static final String APPLICATION_NAME = "test-application";
    public static final String CLIENT_ID = "test-client-id";
    public static final String CLIENT_SECRET = "test-client-secret";
    private static final Charset CHARSET = new Charset(StandardCharsets.UTF_8);

    private static final byte[] ENCRYPTED_TOKEN = new byte[]{0, 0, 0};
    private static final String ENCRYPTED_TOKEN_BASE64 = Base64.getUrlEncoder().withoutPadding().encodeToString(ENCRYPTED_TOKEN);

    private final Github github = mock(Github.class);
    private final EncryptionService encryptionService = mock(EncryptionService.class);

    private final Datastore datastore = DatastoreOptions.newBuilder().setNamespace(APPLICATION_NAME).build().getService();
    private final GithubOAuthService service = new GithubOAuthService(datastore, github, encryptionService, SERVICE_URL,
            new GithubCredentials(CLIENT_ID, CLIENT_SECRET), CHARSET);

    private static Map<String, String> parseQuery(String url) throws MalformedURLException {
        return Arrays.stream(new URL(url).getQuery().split("&"))
                .collect(Collectors.toMap(s -> s.split("=")[0], s -> URLDecoder.decode(s.split("=")[1], CHARSET.getValue())));
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

    @SuppressWarnings("unchecked")
    private Pair<String, String> mockAuthorization(String state) throws IOException {
        Call<JsonObject> call = mock(Call.class);
        Response<JsonObject> response = mock(Response.class);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(Boolean.TRUE);

        String code = UUID.randomUUID().toString();

        String accessToken = UUID.randomUUID().toString();
        String tokenType = "bearer";
        JsonObject responseBody = new JsonObject();
        responseBody.addProperty("access_token", accessToken);
        responseBody.addProperty("token_type", tokenType);
        when(response.body()).thenReturn(responseBody);
        when(encryptionService.encrypt(any(), any())).thenReturn(ENCRYPTED_TOKEN);

        when(github.loginApplication(same(code), eq(state))).thenReturn(Pair.of(tokenType, accessToken));
        return Pair.of(code, accessToken);
    }

    private static String stripAuthToken(String redirectUrl) throws MalformedURLException {
        String redirectUriPath = new URL(redirectUrl).getPath();
        return redirectUriPath.substring(redirectUriPath.lastIndexOf('/') + 1);
    }

    @Test
    public void verify_authorization_success() throws IOException {
        String urlStr = service.createIntegration();
        Map<String, String> urlQuery = parseQuery(urlStr);
        String authToken = stripAuthToken(urlQuery.get("redirect_uri"));
        String state = urlQuery.get("state");

        Pair<String, String> tokens = mockAuthorization(state);

        String userAccessToken = service.authorize(authToken, tokens.getKey(), state);
        assertThat(userAccessToken, not(emptyOrNullString()));

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(encryptionService).encrypt(captor.capture(), any());
        List<byte[]> encryptionValues = captor.getAllValues();
        assertThat(encryptionValues.get(0), equalTo(("bearer" + " " + tokens.getValue()).getBytes(CHARSET.getValue())));

        Key key = datastore.newKeyFactory().setKind("integrations")
                .newKey(new BigInteger(Keys.decodeAuthToken(userAccessToken).getMiddle()).longValue());
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

        assertThat(entity.getString("data"), equalTo(ENCRYPTED_TOKEN_BASE64));
    }

    @Test
    public void verify_authentication_success() throws IOException, InterruptedException {
        String urlStr = service.createIntegration();
        Map<String, String> urlQuery = parseQuery(urlStr);
        String authToken = stripAuthToken(urlQuery.get("redirect_uri"));
        String state = urlQuery.get("state");

        Pair<String, String> tokens = mockAuthorization(state);

        String userAccessToken = service.authorize(authToken, tokens.getKey(), state);

        when(encryptionService.decrypt(any(), any())).thenReturn(tokens.getValue().getBytes(CHARSET.getValue()));

        Thread.sleep(20);

        String result = service.authenticate(userAccessToken);
        assertThat(result, equalTo(tokens.getValue()));

        Key key = datastore.newKeyFactory().setKind("integrations")
                .newKey(new BigInteger(Keys.decodeAuthToken(userAccessToken).getMiddle()).longValue());
        Entity entity = datastore.get(key);

        Date now = Calendar.getInstance().getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -1);
        Date minuteAgo = calendar.getTime();
        Date creationDate = entity.getTimestamp("creationDate").toDate();

        assertThat(entity.getTimestamp("accessDate").toDate(), allOf(greaterThan(minuteAgo),
                lessThanOrEqualTo(now), greaterThan(creationDate)));
    }


    @Test
    public void verify_authentication_failure_invalid_token_format() throws IOException, InterruptedException {
        String urlStr = service.createIntegration();
        Map<String, String> urlQuery = parseQuery(urlStr);
        String authToken = stripAuthToken(urlQuery.get("redirect_uri"));
        String state = urlQuery.get("state");

        Pair<String, String> tokens = mockAuthorization(state);

        service.authorize(authToken, tokens.getKey(), state);

        when(encryptionService.decrypt(any(), any())).thenReturn(tokens.getValue().getBytes(CHARSET.getValue()));

        Thread.sleep(20);

        IllegalArgumentException t = Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.authenticate(UUID.randomUUID().toString()));
        assertThat(t.getMessage(), equalTo("Invalid token"));
    }

    private static String[] randomTokens() {
        return new String[]{Keys.encodeAuthToken(KeyType.STRING, Keys.getBytes(UUID.randomUUID()), Keys.getBytes(UUID.randomUUID())),
                Keys.encodeAuthToken(KeyType.LONG, Keys.getKeyBytes(new Random().nextLong(), CHARSET.getValue()), Keys.getBytes(UUID.randomUUID()))};
    }

    @ParameterizedTest
    @MethodSource("randomTokens")
    public void verify_authentication_failure_no_such_token(String token) throws IOException, InterruptedException {
        String urlStr = service.createIntegration();
        Map<String, String> urlQuery = parseQuery(urlStr);
        String authToken = stripAuthToken(urlQuery.get("redirect_uri"));
        String state = urlQuery.get("state");

        Pair<String, String> tokens = mockAuthorization(state);

        service.authorize(authToken, tokens.getKey(), state);

        when(encryptionService.decrypt(any(), any())).thenReturn(tokens.getValue().getBytes(CHARSET.getValue()));

        Thread.sleep(20);

        AuthenticationFailedException t = Assertions.assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate(token));
        assertThat(t.getMessage(), equalTo("Invalid authentication token"));
    }
}
