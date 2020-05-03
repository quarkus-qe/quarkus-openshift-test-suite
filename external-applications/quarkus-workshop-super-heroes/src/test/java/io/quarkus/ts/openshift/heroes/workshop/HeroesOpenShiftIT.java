package io.quarkus.ts.openshift.heroes.workshop;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.ManualDeployment;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@OpenShiftTest
@ManualDeployment(appName = "quarkus-workshop-hero", httpRoot = "/", knownEndpoint = "/")
@AdditionalResources("classpath:openjdk-11-rhel7.yaml")
@AdditionalResources("classpath:heroes-database.yaml")
@AdditionalResources("classpath:hero.yaml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HeroesOpenShiftIT {

    @TestResource
    private URL url;

    private static String heroId;

    private static final String DEFAULT_NAME = "Test Hero";
    private static final String UPDATED_NAME = "Updated Test Hero";
    private static final String DEFAULT_OTHER_NAME = "Other Test Hero Name";
    private static final String UPDATED_OTHER_NAME = "Updated Other Test Hero Name";
    private static final String DEFAULT_PICTURE = "harold.png";
    private static final String UPDATED_PICTURE = "hackerman.png";
    private static final String DEFAULT_POWERS = "Partakes in this test";
    private static final String UPDATED_POWERS = "Partakes in update test";
    private static final int DEFAULT_LEVEL = 42;
    private static final int UPDATED_LEVEL = 43;

    @Test
    public void testHello() {
        when()
            .get(url + "/api/heroes/hello")
        .then()
            .statusCode(OK.getStatusCode())
            .body(is("hello"));
    }

    @Test
    public void testOpenApi() {
        given()
            .header(ACCEPT, APPLICATION_JSON)
            .when().get(url + "/openapi")
            .then()
            .statusCode(OK.getStatusCode());
    }

    @Test
    public void testLiveness() {
        given()
            .header(ACCEPT, APPLICATION_JSON)
            .when().get(url + "/health/live")
            .then()
            .statusCode(OK.getStatusCode());
    }

    @Test
    public void testReadiness() {
        given()
            .header(ACCEPT, APPLICATION_JSON)
            .when().get(url + "/health/ready")
            .then()
            .statusCode(OK.getStatusCode());
    }

    @Test
    public void testMetrics() {
        given()
            .header(ACCEPT, APPLICATION_JSON)
            .when().get(url + "/metrics/application")
            .then()
            .statusCode(OK.getStatusCode());
    }

    @Test
    @Order(1)
    public void testCreateHero() {
        Hero hero = new Hero();
        hero.name = DEFAULT_NAME;
        hero.otherName = DEFAULT_OTHER_NAME;
        hero.level = DEFAULT_LEVEL;
        hero.picture = DEFAULT_PICTURE;
        hero.powers = DEFAULT_POWERS;

        String location = given()
            .body(hero)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .header(ACCEPT, APPLICATION_JSON)
            .when()
            .post(url + "/api/heroes")
            .then()
            .statusCode(CREATED.getStatusCode())
            .extract().header("Location");
        assertTrue(location.contains("/api/heroes"));


        String[] segments = location.split("/");
        heroId = segments[segments.length - 1];
        assertNotNull(heroId);

        given()
            .pathParam("id", heroId)
            .when().get("/api/heroes/{id}")
            .then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body("name", is(DEFAULT_NAME))
            .body("otherName", is(DEFAULT_OTHER_NAME))
            .body("level", is(DEFAULT_LEVEL*3))
            .body("picture", is(DEFAULT_PICTURE))
            .body("powers", is(DEFAULT_POWERS));
    }

    @Test
    @Order(2)
    public void testUpdateHero() {
        Hero hero = new Hero();
        hero.id = new Long(heroId);
        hero.name = UPDATED_NAME;
        hero.otherName = UPDATED_OTHER_NAME;
        hero.level = UPDATED_LEVEL;
        hero.picture = UPDATED_PICTURE;
        hero.powers = UPDATED_POWERS;

        given()
            .body(hero)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .header(ACCEPT, APPLICATION_JSON)
            .when()
            .put("/api/heroes")
            .then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body("name", is(UPDATED_NAME))
            .body("otherName", is(UPDATED_OTHER_NAME))
            .body("level", is(UPDATED_LEVEL))
            .body("picture", is(UPDATED_PICTURE))
            .body("powers", is(UPDATED_POWERS));
    }

    @Test
    @Order(3)
    public void testDeleteHero() {
        given()
            .pathParam("id", heroId)
            .when().delete("/api/heroes/{id}")
            .then()
            .statusCode(NO_CONTENT.getStatusCode());
    }

    @Test
    @Order(4)
    public void testCalledOperationMetrics() {
        given()
            .header(ACCEPT, APPLICATION_JSON)
            .when().get(url + "/metrics/application")
            .then()
            .statusCode(OK.getStatusCode())
            .body("'io.quarkus.workshop.superheroes.hero.HeroResource.countCreateHero'", is(1))
            .body("'io.quarkus.workshop.superheroes.hero.HeroResource.countUpdateHero'", is(1))
            .body("'io.quarkus.workshop.superheroes.hero.HeroResource.countDeleteHero'", is(1));
    }

    class Hero {
        public Long id;
        public String name;
        public String otherName;
        public int level;
        public String picture;
        public String powers;
    }

}
