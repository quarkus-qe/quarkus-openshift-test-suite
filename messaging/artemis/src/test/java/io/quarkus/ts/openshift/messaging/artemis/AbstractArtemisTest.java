package io.quarkus.ts.openshift.messaging.artemis;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractArtemisTest {

    /**
     * There is a PriceProducer that pushes a new integer "price" to a JMS queue called "prices" each second.
     * PriceConsumer is a loop that starts at the beginning of the application runtime and blocks on reading
     * from the queue called "prices". Once a value is read, the attribute lastPrice is updated.
     *
     * This test merely checks that the value was updated. It is the most basic sanity check that JMS is up
     * and running.
     */
    @Test
    @Order(1)
    public void testLastPrice() {
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            String value =
                    given()
                    .when()
                            .get("/prices/last")
                    .then()
                            .statusCode(200)
                            .extract().body().asString();

            int intValue = Integer.parseInt(value);
            assertThat(intValue, greaterThanOrEqualTo(0));
            assertThat(intValue, lessThan(100));
        });
    }
}
