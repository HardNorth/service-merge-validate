package net.hardnorth.github.merge.api;

import io.quarkus.test.junit.QuarkusTest;
import net.hardnorth.github.merge.config.PropertyNames;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
public class MergeValidateControllerTest {

    public static final String GITHUB_BASE_URL = "http://localhost/";
    public static final String GITHUB_AUTHORIZE_URL = GITHUB_BASE_URL + "login/oauth/authorize";

    static {
        System.setProperty(PropertyNames.APPLICATION_URL, "https://merge.hardnorth.net");
        System.setProperty(PropertyNames.GITHUB_CLIENT_ID, "test_client_id");
        System.setProperty(PropertyNames.GITHUB_CLIENT_SECRET, "test_client_token");
        System.setProperty(PropertyNames.GITHUB_BASE_URL, GITHUB_BASE_URL);
    }

    // just to test the context is running
    @Test
    public void test_healthcheck_endpoint() {
        given()
                .when().get("/healthcheck")
                .then()
                .statusCode(200)
                .body(is(MergeValidateController.class.getSimpleName() + ": OK")).log().everything();
    }

    @Test
    public void test_create_integration_endpoint() {
        given()
                .when().post("/integration")
                .then()
                .statusCode(302)
                .header(HttpHeaders.LOCATION, startsWith(GITHUB_AUTHORIZE_URL)).log().everything();
    }

    // The request will fail, but code '424' means that the call was happened and Github service context were up
    @Test
    public void test_authorize() {
        given()
                .when()
                .queryParam("code", "11111")
                .queryParam("state", "22222")
                .get("/integration/result/3333")
                .then()
                .statusCode(HttpStatus.SC_FAILED_DEPENDENCY).log().everything();
    }
}
