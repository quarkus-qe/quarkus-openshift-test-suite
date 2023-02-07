package io.quarkus.ts.openshift.sqldb.multiplepus;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.ts.openshift.sqldb.multiplepus.model.fruit.Fruit;
import io.quarkus.ts.openshift.sqldb.multiplepus.model.vegetable.Vegetable;
import io.restassured.http.ContentType;

public abstract class AbstractMultiplePersistenceUnitTest {

    private static boolean shouldCleanupFruit = false;
    private static boolean shouldCleanupVegetable = false;
    private static int latestFruitId = 7;
    private static int latestVegetableId = 7;

    @BeforeEach
    public void cleanUp() {
        if (shouldCleanupFruit) {
            when()
                    .delete("/fruit/" + latestFruitId)
                    .then()
                    .statusCode(NO_CONTENT.getStatusCode());
            shouldCleanupFruit = false;
        }
        if (shouldCleanupVegetable) {
            when()
                    .delete("/vegetable/" + latestVegetableId)
                    .then()
                    .statusCode(NO_CONTENT.getStatusCode());
            shouldCleanupVegetable = false;
        }
    }

    @Test
    public void getAllFruits() {
        when()
                .get("/fruit")
                .then()
                .statusCode(OK.getStatusCode())
                .body("", hasSize(7));
    }

    @Test
    public void getFruitById() {
        when()
                .get("/fruit/7")
                .then()
                .statusCode(OK.getStatusCode())
                .body("name", equalTo("Cranberry"));
    }

    @Test
    public void createFruit() {
        Fruit fruit = new Fruit();
        fruit.name = "Canteloupe";

        latestFruitId++;

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(fruit)
                .post("/fruit")
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("id", equalTo(latestFruitId))
                .body("name", equalTo("Canteloupe"));
        shouldCleanupFruit = true;

        when()
                .get("/fruit/" + latestFruitId)
                .then()
                .statusCode(OK.getStatusCode())
                .body("name", equalTo("Canteloupe"));
    }

    @Test
    public void createInvalidPayloadFruit() {
        given()
                .when()
                .contentType(ContentType.TEXT)
                .body("")
                .post("/fruit")
                .then()
                .statusCode(UNSUPPORTED_MEDIA_TYPE.getStatusCode())
                .body("code", equalTo(UNSUPPORTED_MEDIA_TYPE.getStatusCode()));
    }

    @Test
    public void createInvalidIdFruit() {
        Fruit fruit = new Fruit();
        fruit.id = 999L;
        fruit.name = "foo";

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(fruit)
                .post("/fruit")
                .then()
                .statusCode(422)
                .body("code", equalTo(422))
                .body("error", equalTo("unexpected ID in request"));
    }

    @Test
    public void updateFruit() {
        Fruit fruit = new Fruit();
        fruit.name = "Canteloupe";

        latestFruitId++;

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(fruit)
                .post("/fruit")
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("id", equalTo(latestFruitId))
                .body("name", equalTo("Canteloupe"));

        Fruit updatedFruit = new Fruit();
        updatedFruit.id = (long) latestFruitId;
        updatedFruit.name = "Dragonfruit";

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(updatedFruit)
                .put("/fruit/" + latestFruitId)
                .then()
                .statusCode(OK.getStatusCode())
                .body("id", equalTo(latestFruitId))
                .body("name", equalTo("Dragonfruit"));
        shouldCleanupFruit = true;
        when()
                .get("/fruit/" + latestFruitId)
                .then()
                .statusCode(OK.getStatusCode())
                .body("name", equalTo("Dragonfruit"));
    }

    @Test
    public void updateFruitWithUnknownId() {
        Fruit fruit = new Fruit();
        fruit.id = 999L;
        fruit.name = "foo";

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(fruit)
                .put("/fruit/999")
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("code", equalTo(NOT_FOUND.getStatusCode()))
                .body("error", equalTo("fruit '999' not found"));
    }

    @Test
    public void updateWithNullFruit() {
        given()
                .when()
                .contentType(ContentType.TEXT)
                .body("")
                .put("/fruit/" + latestFruitId)
                .then()
                .statusCode(UNSUPPORTED_MEDIA_TYPE.getStatusCode())
                .body("code", equalTo(UNSUPPORTED_MEDIA_TYPE.getStatusCode()));
    }

    @Test
    public void updateWithInvalidFruit() {
        Fruit fruit = new Fruit();

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(fruit)
                .put("/fruit/" + latestFruitId)
                .then()
                .statusCode(422)
                .body("code", equalTo(422))
                .body("error.message", contains("Fruit name must be set!"));
    }

    @Test
    public void deleteFruit() {
        Fruit fruit = new Fruit();
        fruit.name = "Canteloupe";

        latestFruitId++;

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(fruit)
                .post("/fruit")
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("id", equalTo(latestFruitId))
                .body("name", equalTo("Canteloupe"));

        when()
                .delete("/fruit/" + latestFruitId)
                .then()
                .statusCode(NO_CONTENT.getStatusCode());

        when()
                .get("/fruit/" + latestFruitId)
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("code", equalTo(NOT_FOUND.getStatusCode()))
                .body("error", equalTo("fruit '" + latestFruitId + "' not found"));
    }

    @Test
    public void deleteFruitWithUnknownId() {
        when()
                .delete("/fruit/999")
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("code", equalTo(NOT_FOUND.getStatusCode()))
                .body("error", equalTo("fruit '999' not found"));
    }

    @Test
    public void getAllVegetables() {
        when()
                .get("/vegetable")
                .then()
                .statusCode(OK.getStatusCode())
                .body("", hasSize(7));
    }

    @Test
    public void getVegetableById() {
        when()
                .get("/vegetable/7")
                .then()
                .statusCode(OK.getStatusCode())
                .body("name", equalTo("Garlic"));
    }

    @Test
    public void createVegetable() {
        Vegetable vegetable = new Vegetable();
        vegetable.name = "Eggplant";

        latestVegetableId++;

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(vegetable)
                .post("/vegetable")
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("id", equalTo(latestVegetableId))
                .body("name", equalTo("Eggplant"));
        shouldCleanupVegetable = true;
        when()
                .get("/vegetable/" + latestVegetableId)
                .then()
                .statusCode(OK.getStatusCode())
                .body("name", equalTo("Eggplant"));
    }

    @Test
    public void createNullVegetable() {
        given()
                .when()
                .contentType(ContentType.TEXT)
                .body("")
                .post("/vegetable")
                .then()
                .statusCode(UNSUPPORTED_MEDIA_TYPE.getStatusCode())
                .body("code", equalTo(UNSUPPORTED_MEDIA_TYPE.getStatusCode()));
    }

    @Test
    public void createInvalidIdVegetable() {
        Vegetable vegetable = new Vegetable();
        vegetable.id = 999L;
        vegetable.name = "foo";

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(vegetable)
                .post("/vegetable")
                .then()
                .statusCode(422)
                .body("code", equalTo(422))
                .body("error", equalTo("unexpected ID in request"));
    }

    @Test
    public void updateVegetable() {
        Vegetable vegetable = new Vegetable();
        vegetable.name = "Eggplant";

        latestVegetableId++;

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(vegetable)
                .post("/vegetable")
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("id", equalTo(latestVegetableId))
                .body("name", equalTo("Eggplant"));

        Vegetable updatedVegetable = new Vegetable();
        updatedVegetable.id = (long) latestVegetableId;
        updatedVegetable.name = "Okra";

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(updatedVegetable)
                .put("/vegetable/" + latestVegetableId)
                .then()
                .statusCode(OK.getStatusCode())
                .body("id", equalTo(latestVegetableId))
                .body("name", equalTo("Okra"));

        shouldCleanupVegetable = true;
        when()
                .get("/vegetable/" + latestVegetableId)
                .then()
                .statusCode(OK.getStatusCode())
                .body("name", equalTo("Okra"));
    }

    @Test
    public void updateVegetableWithUnknownId() {
        Vegetable vegetable = new Vegetable();
        vegetable.id = 999L;
        vegetable.name = "foo";

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(vegetable)
                .put("/vegetable/999")
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("code", equalTo(NOT_FOUND.getStatusCode()))
                .body("error", equalTo("vegetable '999' not found"));
    }

    @Test
    public void updateWithNullVegetable() {
        given()
                .when()
                .contentType(ContentType.TEXT)
                .body("")
                .put("/vegetable/" + latestVegetableId)
                .then()
                .statusCode(UNSUPPORTED_MEDIA_TYPE.getStatusCode())
                .body("code", equalTo(UNSUPPORTED_MEDIA_TYPE.getStatusCode()));
    }

    @Test
    public void updateWithInvalidVegetable() {
        Vegetable vegetable = new Vegetable();

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(vegetable)
                .put("/vegetable/" + latestVegetableId)
                .then()
                .statusCode(422)
                .body("code", equalTo(422))
                .body("error.message", contains("Vegetable name must be set!"));
    }

    @Test
    public void deleteVegetable() {
        Vegetable vegetable = new Vegetable();
        vegetable.name = "Eggplant";

        latestVegetableId++;

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(vegetable)
                .post("/vegetable")
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("id", equalTo(latestVegetableId))
                .body("name", equalTo("Eggplant"));

        when()
                .delete("/vegetable/" + latestVegetableId)
                .then()
                .statusCode(NO_CONTENT.getStatusCode());

        when()
                .get("/vegetable/" + latestVegetableId)
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("code", equalTo(NOT_FOUND.getStatusCode()))
                .body("error", equalTo("vegetable '" + latestVegetableId + "' not found"));
    }

    @Test
    public void deleteVegetableWithUnknownId() {
        when()
                .delete("/vegetable/999")
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("code", equalTo(NOT_FOUND.getStatusCode()))
                .body("error", equalTo("vegetable '999' not found"));
    }
}
