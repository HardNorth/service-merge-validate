package net.hardnorth.github.merge.api;

import io.quarkus.test.junit.QuarkusTest;
import net.hardnorth.github.merge.config.PropertyNames;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class MergeValidateControllerTest {

    static {
        System.setProperty(PropertyNames.APPLICATION_URL, "https://merge.hardnorth.net");
        System.setProperty(PropertyNames.GITHUB_CLIENT_ID, "test_client_id");
        System.setProperty(PropertyNames.GITHUB_CLIENT_SECRET, "test_client_token");
    }

    @Test
    // just to test the context is running
    public void testPingEndpoint() {
        given()
                .when().get("/healthcheck")
                .then()
                .statusCode(200)
                .body(is(MergeValidateController.class.getSimpleName()+ ": OK"));
    }
}
