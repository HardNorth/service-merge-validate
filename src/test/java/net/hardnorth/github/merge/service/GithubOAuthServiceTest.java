package net.hardnorth.github.merge.service;

import com.google.cloud.datastore.*;
import net.hardnorth.github.merge.model.GithubCredentials;
import net.hardnorth.github.merge.service.impl.GithubOAuthService;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class GithubOAuthServiceTest {
    public static final String PROJECT_ID = "test";
    public static final int SERVICE_PORT = 8889;
    public static final String SERVICE_URL = "http://localhost:" + SERVICE_PORT;
    public static final String GITHUB_URL = "https://github.com/login/oauth/authorize";
    public static final String APPLICATION_NAME = "test-application";
    public static final String CLIENT_ID = "test-client-id";
    public static final String CLIENT_SECRET = "test-client-secret";

    private final GithubClient github = mock(GithubClient.class);
    private final EncryptedStorage storage = mock(EncryptedStorage.class);

    private final Datastore datastore = DatastoreOptions.newBuilder().setNamespace(APPLICATION_NAME).build().getService();
    private final GithubOAuthService service = new GithubOAuthService(datastore, github, storage, SERVICE_URL,
            new GithubCredentials(CLIENT_ID, CLIENT_SECRET));

    @Test
    public void verify_createIntegration_url() throws MalformedURLException {
        String urlStr = service.createIntegration();

        assertThat(urlStr, startsWith(GITHUB_URL));

        URL url = new URL(urlStr);
        Map<String, String> query = Arrays.stream(url.getQuery().split("&"))
                .collect(Collectors.toMap(s -> s.split("=")[0], s -> s.split("=")[1]));
        assertThat(query, hasEntry(equalTo("redirect_uri"),
                startsWith(URLEncoder.encode(SERVICE_URL + "/integration/result/", StandardCharsets.UTF_8))
        ));
        assertThat(query, hasEntry(equalTo("state"), not(emptyOrNullString())));
        assertThat(query, hasEntry(equalTo("client_id"), equalTo(URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8))));
        assertThat(query, hasEntry(equalTo("scope"), equalTo(URLEncoder.encode("repo user:email", StandardCharsets.UTF_8))));
    }

    @Test
    public void verify_createIntegration_database_entry_creation() throws MalformedURLException {
        String urlStr = service.createIntegration();

        URL url = new URL(urlStr);
        Map<String, String> urlQuery = Arrays.stream(url.getQuery().split("&"))
                .collect(Collectors.toMap(s -> s.split("=")[0], s -> URLDecoder.decode(s.split("=")[1], StandardCharsets.UTF_8)));
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
}
