package io.quarkus.ts.openshift.config.secret.api.server;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HelloTest {
    @Test
    public void hello() {
        when()
                .get("/hello")
                .then()
                .statusCode(200)
                .body("content", is("Hello, World!"));
    }
}
