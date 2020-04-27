package io.quarkus.ts.openshift.jms;

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
public abstract class AbstractJMSTest {
    @Test
    @Order(1)
    public void testLastPrice() {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
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
