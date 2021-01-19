package net.hardnorth.github.merge.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class MergeValidateControllerTest {
    @Test
    public void testPingEndpoint() {
        given()
                .when().body("hello").post("/ping")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }
}
