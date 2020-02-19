package io.quarkus.ts.openshift.http;

import io.quarkus.ts.openshift.common.OpenShiftTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@OpenShiftTest
public class HttpOpenShiftIT {
    @Test
    public void hello() {
        when()
                .get("/hello")
        .then()
                .statusCode(200)
                .body("content", is("Hello, World!"));
    }
}
