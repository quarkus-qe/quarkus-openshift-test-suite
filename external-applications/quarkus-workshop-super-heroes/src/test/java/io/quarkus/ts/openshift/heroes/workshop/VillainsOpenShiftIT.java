package io.quarkus.ts.openshift.heroes.workshop;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.CustomAppMetadata;
import io.quarkus.ts.openshift.common.ManualApplicationDeployment;
import io.quarkus.ts.openshift.common.OnlyIfConfigured;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;
import org.hamcrest.core.Is;
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
@ManualApplicationDeployment
@CustomAppMetadata(appName = "quarkus-workshop-villain", httpRoot = "/", knownEndpoint = "/")
@AdditionalResources("classpath:openjdk-11-rhel7.yaml")
@AdditionalResources("classpath:villains-database.yaml")
@AdditionalResources("classpath:villain.yaml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@OnlyIfConfigured("ts.authenticated-registry")
public class VillainsOpenShiftIT {

    @TestResource
    private URL url;

    private static String villainId;

    private static final String DEFAULT_NAME = "Test Villain";
    private static final String UPDATED_NAME = "Updated Test Villain";
    private static final String DEFAULT_OTHER_NAME = "Other Test Villain Name";
    private static final String UPDATED_OTHER_NAME = "Updated Other Test Villain Name";
    private static final String DEFAULT_PICTURE = "harold.png";
    private static final String UPDATED_PICTURE = "hackerman.png";
    private static final String DEFAULT_POWERS = "Partakes in this test";
    private static final String UPDATED_POWERS = "Partakes in update test";
    private static final int DEFAULT_LEVEL = 42;
    private static final int UPDATED_LEVEL = 43;

    @Test
    public void testHello() {
        when()
            .get(url + "/api/villains/hello")
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
    public void testCreateVillain() {
        Villain villain = new Villain();
        villain.name = DEFAULT_NAME;
        villain.otherName = DEFAULT_OTHER_NAME;
        villain.level = DEFAULT_LEVEL;
        villain.picture = DEFAULT_PICTURE;
        villain.powers = DEFAULT_POWERS;

        String location = given()
            .body(villain)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .header(ACCEPT, APPLICATION_JSON)
            .when()
            .post(url + "/api/villains")
            .then()
            .statusCode(CREATED.getStatusCode())
            .extract().header("Location");
        assertTrue(location.contains("/api/villains"));


        String[] segments = location.split("/");
        villainId = segments[segments.length - 1];
        assertNotNull(villainId);

        given()
            .pathParam("id", villainId)
            .when().get("/api/villains/{id}")
            .then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body("name", is(DEFAULT_NAME))
            .body("otherName", is(DEFAULT_OTHER_NAME))
            .body("level", is(DEFAULT_LEVEL*2))
            .body("picture", is(DEFAULT_PICTURE))
            .body("powers", is(DEFAULT_POWERS));
    }

    @Test
    @Order(2)
    public void testUpdateVillain() {
        Villain villain = new Villain();
        villain.id = Long.valueOf(villainId);
        villain.name = UPDATED_NAME;
        villain.otherName = UPDATED_OTHER_NAME;
        villain.level = UPDATED_LEVEL;
        villain.picture = UPDATED_PICTURE;
        villain.powers = UPDATED_POWERS;

        given()
            .body(villain)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .header(ACCEPT, APPLICATION_JSON)
            .when()
            .put("/api/villains")
            .then()
            .statusCode(OK.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body("name", Is.is(UPDATED_NAME))
            .body("otherName", Is.is(UPDATED_OTHER_NAME))
            .body("level", Is.is(UPDATED_LEVEL))
            .body("picture", Is.is(UPDATED_PICTURE))
            .body("powers", Is.is(UPDATED_POWERS));
    }

    @Test
    @Order(3)
    public void testDeleteVillain() {
        given()
            .pathParam("id", villainId)
            .when().delete("/api/villains/{id}")
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
            .body("'io.quarkus.workshop.superheroes.villain.VillainResource.countCreateVillain'", is(1))
            .body("'io.quarkus.workshop.superheroes.villain.VillainResource.countUpdateVillain'", is(1))
            .body("'io.quarkus.workshop.superheroes.villain.VillainResource.countDeleteVillain'", is(1));
    }

    static class Villain {
        public Long id;
        public String name;
        public String otherName;
        public int level;
        public String picture;
        public String powers;
    }

}
