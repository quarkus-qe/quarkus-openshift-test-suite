package io.quarkus.ts.openshift.sqldb;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.restassured.http.ContentType;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractSqlDatabaseTest {
    @Test
    @Order(1)
    public void getAll() {
        when()
                .get("/book")
                .then()
                .statusCode(200)
                .body("", hasSize(7));
    }

    @Test
    @Order(2)
    public void get() {
        when()
                .get("/book/7")
                .then()
                .statusCode(200)
                .body("title", equalTo("Perdido Street Station"))
                .body("author", equalTo("China Miéville"));
    }

    @Test
    @Order(3)
    public void create() {
        Book book = new Book();
        book.title = "Neuromancer";
        book.author = "William Gibson";

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(book)
                .post("/book")
                .then()
                .statusCode(201)
                .body("id", equalTo(8))
                .body("title", equalTo("Neuromancer"))
                .body("author", equalTo("William Gibson"));

        when()
                .get("/book/8")
                .then()
                .statusCode(200)
                .body("title", equalTo("Neuromancer"))
                .body("author", equalTo("William Gibson"));
    }

    @Test
    @Order(4)
    public void createInvalidPayload() {
        given()
                .when()
                .contentType(ContentType.TEXT)
                .body("")
                .post("/book")
                .then()
                .statusCode(415)
                .body("code", equalTo(415));
    }

    @Test
    @Order(5)
    public void createBadPayload() {
        Book book = new Book();
        book.id = 999L;
        book.title = "foo";
        book.author = "bar";

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(book)
                .post("/book")
                .then()
                .statusCode(422)
                .body("code", equalTo(422))
                .body("error", equalTo("unexpected ID in request"));
    }

    @Test
    @Order(6)
    public void update() {
        Book book = new Book();
        book.id = 8L;
        book.title = "Schismatrix";
        book.author = "Bruce Sterling";

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(book)
                .put("/book/8")
                .then()
                .statusCode(200)
                .body("id", equalTo(8))
                .body("title", equalTo("Schismatrix"))
                .body("author", equalTo("Bruce Sterling"));

        when()
                .get("/book/8")
                .then()
                .statusCode(200)
                .body("title", equalTo("Schismatrix"))
                .body("author", equalTo("Bruce Sterling"));
    }

    @Test
    @Order(7)
    public void updateWithUnknownId() {
        Book book = new Book();
        book.id = 999L;
        book.title = "foo";
        book.author = "bar";

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(book)
                .put("/book/999")
                .then()
                .statusCode(404)
                .body("code", equalTo(404))
                .body("error", equalTo("book '999' not found"));
    }

    @Test
    @Order(8)
    public void updateInvalidPayload() {
        given()
                .when()
                .contentType(ContentType.TEXT)
                .body("")
                .put("/book/8")
                .then()
                .statusCode(415)
                .body("code", equalTo(415));
    }

    @Test
    @Order(9)
    public void updateBadPayload() {
        Book book = new Book();

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(book)
                .put("/book/8")
                .then()
                .statusCode(422)
                .body("code", equalTo(422))
                .body("error.message", containsInAnyOrder("book title must be set", "book author must be set"));
    }

    @Test
    @Order(10)
    public void delete() {
        when()
                .delete("/book/8")
                .then()
                .statusCode(204);

        when()
                .get("/book/8")
                .then()
                .statusCode(404)
                .body("code", equalTo(404))
                .body("error", equalTo("book '8' not found"));
    }

    @Test
    @Order(11)
    public void deleteWithUnknownId() {
        when()
                .delete("/book/999")
                .then()
                .statusCode(404)
                .body("code", equalTo(404))
                .body("error", equalTo("book '999' not found"));
    }
}
