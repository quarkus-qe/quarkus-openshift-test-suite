package io.quarkus.ts.openshift.http;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

public abstract class AbstractHttpTest {
    @Test
    public void httpServer() {
        when()
                .get("/hello")
                .then()
                .statusCode(200)
                .body("content", is("Hello, World!"));
    }
}
