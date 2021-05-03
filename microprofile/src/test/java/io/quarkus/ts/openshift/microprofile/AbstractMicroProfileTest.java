package io.quarkus.ts.openshift.microprofile;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.with;
import static org.hamcrest.CoreMatchers.is;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractMicroProfileTest {
    @Test
    @Order(1)
    public void hello() {
        with().pollInterval(Duration.ofSeconds(1)).and()
                .with().pollDelay(Duration.ofSeconds(10)).await()
                .atLeast(Duration.ofSeconds(1))
                .atMost(59, TimeUnit.SECONDS)
                .with()
                .untilAsserted(() -> {
                    when()
                            .get("/client")
                            .then()
                            .statusCode(200)
                            .log().body()
                            .log().status()
                            .body(is("Client got: Hello, World!"));
                });
    }

    @Test
    @Order(10)
    @Disabled("https://issues.redhat.com/browse/QUARKUS-697")
    public void fallback() {
        with().pollInterval(Duration.ofSeconds(1)).and()
                .with().pollDelay(Duration.ofSeconds(10)).await()
                .atLeast(Duration.ofSeconds(1))
                .atMost(59, TimeUnit.SECONDS)
                .with()
                .untilAsserted(() -> {
                    when()
                            .get("/client/fallback")
                            .then()
                            .log().body()
                            .log().status()
                            .statusCode(200)
                            .body(is("Client got: Fallback"));
                });
    }
}
