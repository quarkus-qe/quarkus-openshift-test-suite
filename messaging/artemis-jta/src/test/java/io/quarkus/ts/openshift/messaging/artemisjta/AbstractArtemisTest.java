package io.quarkus.ts.openshift.messaging.artemisjta;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
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

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat("testPrice:",
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
}
