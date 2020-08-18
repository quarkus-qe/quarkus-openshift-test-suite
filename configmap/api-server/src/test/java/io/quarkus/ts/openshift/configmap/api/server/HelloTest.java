package io.quarkus.ts.openshift.configmap.api.server;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

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
