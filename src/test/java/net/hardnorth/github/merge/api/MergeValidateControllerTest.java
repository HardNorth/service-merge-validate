package net.hardnorth.github.merge.api;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.service.SecretManager;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
public class MergeValidateControllerTest {

    public static final String GITHUB_BASE_URL = "http://localhost/";
    public static final String GITHUB_AUTHORIZE_URL = GITHUB_BASE_URL + "login/oauth/authorize";
    public static final String TEST_CLIENT_ID = "test_client_id";
    public static final String GITHUB_CLIENT_TOKEN = "test_client_token";
    public static final String GITHUB_ENCRYPTION_KEY = "test_encryption_key";

    static {
        System.setProperty(PropertyNames.APPLICATION_URL, "https://merge.hardnorth.net");
        System.setProperty(PropertyNames.GITHUB_CLIENT_ID_SECRET, TEST_CLIENT_ID);
        System.setProperty(PropertyNames.GITHUB_CLIENT_TOKEN_SECRET, GITHUB_CLIENT_TOKEN);
        System.setProperty(PropertyNames.GITHUB_ENCRYPTION_KEY_SECRET, GITHUB_ENCRYPTION_KEY);
        System.setProperty(PropertyNames.GITHUB_BASE_URL, GITHUB_BASE_URL);
    }

    @BeforeAll
    public static void setupMock() {
        SecretManager secretManager = mock(SecretManager.class);
        when(secretManager.getSecrets(eq(TEST_CLIENT_ID), eq(GITHUB_CLIENT_TOKEN)))
                .thenReturn(Arrays.asList(TEST_CLIENT_ID, GITHUB_CLIENT_TOKEN));
        when(secretManager.getSecrets(eq(GITHUB_ENCRYPTION_KEY)))
                .thenReturn(Collections.singletonList(Base64.getEncoder().encodeToString(GITHUB_ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8))));
        QuarkusMock.installMockForType(secretManager, SecretManager.class);
    }

    // just to test the context is running
    @Test
    public void test_healthcheck_endpoint() {
        given()
                .when().get("/healthcheck")
                .then()
                .statusCode(200)
                .body(is(MergeValidateController.class.getSimpleName() + ": OK"));
    }

    @Test
    public void test_create_integration_endpoint() {
        given()
                .when().post("/integration")
                .then()
                .statusCode(302)
                .header(HttpHeaders.LOCATION, startsWith(GITHUB_AUTHORIZE_URL));
    }

    // The request will fail, but code '401' means that the call was happened and Github service context were up
    @Test
    public void test_authorize() {
        given()
                .when()
                .queryParam("code", "11111")
                .queryParam("state", "22222")
                .get("/integration/result/AAcSSY81gAAAjoId_mI6Q3moVpishyO0iw")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}
