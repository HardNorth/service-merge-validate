package net.hardnorth.github.merge.api;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import net.hardnorth.github.merge.config.PropertyNames;
import net.hardnorth.github.merge.service.SecretManager;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
public class MergeValidateControllerTest {

    public static final String GITHUB_BASE_URL = "http://localhost/";
    public static final String GITHUB_WEBHOOK_TOKEN_KEY = "test_webhook_token";
    public static final String GITHUB_WEBHOOK_TOKEN = "0795d41d-6a54-42d7-80a0-07c98e5a9688";

    static {
        System.setProperty(PropertyNames.APPLICATION_URL, "https://merge.hardnorth.net");
        System.setProperty(PropertyNames.GITHUB_BASE_URL, GITHUB_BASE_URL);
        System.setProperty(PropertyNames.GITHUB_WEBHOOK_TOKEN_SECRET, GITHUB_WEBHOOK_TOKEN_KEY);
    }

    @BeforeAll
    public static void setupMock() {
        SecretManager secretManager = mock(SecretManager.class);
        when(secretManager.getRawSecret(eq(GITHUB_WEBHOOK_TOKEN_KEY)))
                .thenReturn(GITHUB_WEBHOOK_TOKEN.getBytes(StandardCharsets.UTF_8));
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
    public void web_hook_test() {
        given()
                .body(getClass().getClassLoader().getResourceAsStream("hook/new_installation.json"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header("x-hub-signature-256","sha256=2a30244bfdbd9025f674fb9e18f54ca9b0ff773934ab5d08c416be7f03309122")
                .when()
                .post("/webhook")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }
}
