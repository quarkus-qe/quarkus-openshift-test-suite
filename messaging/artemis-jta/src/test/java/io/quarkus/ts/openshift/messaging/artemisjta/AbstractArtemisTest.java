package io.quarkus.ts.openshift.messaging.artemisjta;

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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractArtemisTest {
    @Test
    @Order(1)
    public void testPrice() {
        given().queryParam("fail", "false").body("666")
                .when()
                .post("/price").then().statusCode(200);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat("testPrice:",
                given()
                        .when()
                        .get("/price")
                        .then()
                        .statusCode(200)
                        .extract().body().asString()
                , equalTo("666:666")));
    }

    @Test
    @Order(2)
    public void testJTAPriceFail() {
        given().queryParam("fail", "true").body("999")
                .when()
                .post("/price").then().statusCode(500);
        Set<String> s = new HashSet<>(10);
        // Any Awaitility positive person to refactor this for loop? :-)
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
        assertThat("Only one queue was updated and that should not have happened.", !s.contains("999:"));
        assertThat("Empty reading from both queues was expected to occur.", s.contains(":"));
    }


    @Test
    @Order(3)
    public void testPriceFail() {
        given().queryParam("fail", "true").body("69")
                .when()
                .post("/NoJTAPrice").then().statusCode(500);
        Set<String> s = new HashSet<>(10);
        // Any Awaitility positive person to refactor this for loop? :-)
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
        assertThat("One queue should have been updated.", s.contains("69:"));
    }

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
        // Any Awaitility positive person to refactor this for loop? :-)
        for (int i = 0; i < size; i++) {
            actual.add(given().queryParam("ack", Boolean.toString(ack))
                    .when()
                    .get("/noAck")
                    .then()
                    .statusCode(200)
                    .extract().body().asString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        assertThat("Expected list " + expected.toString() + " does not match th actual one: " + actual.toString(),
                expected.equals(actual));
    }
}
