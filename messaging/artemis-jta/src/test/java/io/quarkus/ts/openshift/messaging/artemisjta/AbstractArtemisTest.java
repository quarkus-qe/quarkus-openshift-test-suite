package io.quarkus.ts.openshift.messaging.artemisjta;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Synopsis:
 * There are three JMS queues, custom-prices-1 and custom-prices-2 are used to test
 * a transactional write: either both are correctly updated with a new value or none of them is.
 *
 * custom-prices-cack queue is used to check that messages remains waiting in the queue until
 * client "acks" them, i.e. acknowledges their processing.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractArtemisTest {

    private static final Logger LOG = Logger.getLogger(AbstractArtemisTest.class.getName());

    /**
     * The ConsumerService holds state between tests. We clean it here.
     */
    @BeforeEach
    public void clean() {
        given().when().delete("/price").then().statusCode(200);
    }

    /**
     * Number 666 is written to both queues custom-prices-1 and custom-prices-2 within a transaction.
     * The consumer is reading both queues each second and it should at some point see these messages
     * and report 666 from both of them. There is no injected failure here.
     */
    @Test
    @Order(1)
    public void testPrice() {
        given().queryParam("fail", "false")
                .queryParam("transactional", "true")
                .body("666")
                .when()
                .post("/price").then().statusCode(200);

        await().pollInterval(1, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat("testPrice:",
                given()
                        .when()
                        .get("/price")
                        .then()
                        .statusCode(200)
                        .extract().body().asString()
                , equalTo("666:666")));
    }

    /**
     * As above, except there is an error between writing to custom-prices-1 and custom-prices-2 now.
     * Being wrapped in a transaction, it is expected that the write to custom-prices-1 is not committed
     * and no value was written to either of those queues.
     */
    @Test
    @Order(2)
    public void testJTAPriceFail() {
        given().queryParam("fail", "true")
                .queryParam("transactional", "true")
                .body("999")
                .when()
                .post("/price").then().statusCode(500);

        Set<String> s = new HashSet<>(10);
        for (int i = 0; i < 10; i++) {
            s.add(given()
                    .when()
                    .get("/price")
                    .then()
                    .statusCode(200)
                    .extract().body().asString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOG.info("Retrieved data from queues: " + s);
        assertThat("Only one queue was updated and that should not have happened." +
                " Retrieved data from queues: " + s, !s.contains("999:"));
        assertThat("Empty reading from both queues was expected to occur." +
                " Retrieved data from queues: " + s, s.contains(":"));
    }

    /**
     * Continuation of the above. This time there is no transaction and there is an error
     * between writing to both queues. Not being wrapped in atransaction means that there will
     * be an expected inconsistency: one queue gets updated while the other doesn't.
     */
    @Test
    @Order(3)
    public void testPriceFail() {
        given().queryParam("fail", "true", "transactional", "false")
                .body("69")
                .when()
                .post("/price").then().statusCode(500);

        Set<String> s = new HashSet<>(10);
        for (int i = 0; i < 10; i++) {
            s.add(given()
                    .when()
                    .get("/price")
                    .then()
                    .statusCode(200)
                    .extract().body().asString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOG.info("Retrieved data from queues: " + s);
        assertThat("One queue should have been updated." +
                " Retrieved data from queues: " + s, s.contains("69:"));
    }

    /**
     * Above examples use autoack, i.e. a message is assumed acked as soon as it is read from the queue.
     * Here we rely on the consumer, on the client to explicitly ack each message read from the queue.
     * The tests checks that even after reading them, all messages remain in the queue until we ack them.
     */
    @Test
    @Order(4)
    public void testClientAck() {
        List<String> initial = Arrays.asList("96-0", "96-1", "96-2", "96-3", "96-4");
        initial.forEach(p -> given().body(p).when().post("/noAck").then().statusCode(200));
        clientAck(initial.size(), Arrays.asList("96-0", "96-0", "96-0", "96-0", "96-0"), false);
        clientAck(initial.size(), Arrays.asList("96-0", "96-1", "96-2", "96-3", "96-4"), true);
    }

    private void clientAck(int size, List<String> expected, boolean ack) {
        List<String> actual = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            actual.add(given().queryParam("ack", ack)
                    .when()
                    .get("/noAck")
                    .then()
                    .statusCode(200)
                    .extract().body().asString());
        }
        LOG.info("Retrieved data from queues: " + actual);
        assertThat("Expected list " + expected.toString() + " does not match th actual one: " + actual.toString(),
                expected.equals(actual));
    }
}
