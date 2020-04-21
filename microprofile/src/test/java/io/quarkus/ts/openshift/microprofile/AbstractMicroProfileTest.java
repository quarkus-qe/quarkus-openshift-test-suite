package io.quarkus.ts.openshift.microprofile;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractMicroProfileTest {
    @Test
    @Order(1)
    public void hello() {
        when()
                .post("/hello/enable")
        .then()
                .statusCode(204);

        when()
                .get("/client")
        .then()
                .statusCode(200)
                .body(is("Client got: Hello, World!"));
    }

    @Test
    @Order(10)
    @Disabled("https://github.com/quarkusio/quarkus/issues/8650")
    public void fallback() {
        when()
                .post("/hello/disable")
        .then()
                .statusCode(204);

        when()
                .get("/client")
        .then()
                .statusCode(200)
                .body(is("Client got: Fallback"));
    }
}
