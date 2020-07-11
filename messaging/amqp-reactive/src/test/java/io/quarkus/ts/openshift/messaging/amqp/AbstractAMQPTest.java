package io.quarkus.ts.openshift.messaging.amqp;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractAMQPTest {

    @Test
    @Order(1)
    public void testLastPrice() {
        await().pollInterval(1, TimeUnit.SECONDS)
                .atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    when()
                        .get("/price")
                    .then()
                        .statusCode(200)
                        .body(equalTo("[10, 20, 30, 40, 50, 60, 70, 80, 90, 100]")));
    }
}
