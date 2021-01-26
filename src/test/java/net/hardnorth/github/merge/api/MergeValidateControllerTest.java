package net.hardnorth.github.merge.api;

import io.quarkus.test.junit.QuarkusTest;
import net.hardnorth.github.merge.config.PropertyNames;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
public class MergeValidateControllerTest {

    static {
        System.setProperty(PropertyNames.APPLICATION_URL, "https://merge.hardnorth.net");
        System.setProperty(PropertyNames.GITHUB_CLIENT_ID, "test_client_id");
        System.setProperty(PropertyNames.GITHUB_CLIENT_SECRET, "test_client_token");
    }


    public static final String GITHUB_URL = "https://github.com/login/oauth/authorize";

    @Test
    // just to test the context is running
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
                .header(HttpHeaders.LOCATION, startsWith(GITHUB_URL));
    }
}
