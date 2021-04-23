package io.quarkus.ts.openshift.messaging.amqp;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractAMQPTest {

    private static final List<String> EXPECTED_PRICES = Arrays.asList("10", "20", "30", "40", "50", "60", "70", "80", "90", "100");

    /**
     * The producer sends a price every 1 sec {@link io.quarkus.ts.openshift.messaging.amqp.PriceProducer#generate()}.
     * Eventually, the consumer will get up to 10 prices (from 10 to 100) but it might receive more
     * {@link io.quarkus.ts.openshift.messaging.amqp.PriceConsumer#process()}.
     *
     */
    @Test
    @Order(1)
    public void testLastPrice() {
        await().pollInterval(1, TimeUnit.SECONDS)
                .atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            String response = when().get("/price")
                    .then().statusCode(200).extract().asString();
            assertTrue(EXPECTED_PRICES.stream().anyMatch(response::contains), "Expected prices not found in " + response);
        });
    }
}
