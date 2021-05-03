package io.quarkus.ts.openshift.config.secret.file.system;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;

@OpenShiftTest
@AdditionalResources("classpath:secret.yaml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConfigSecretFileSystemOpenShiftIT {
    @TestResource
    private AppMetadata metadata;

    @TestResource
    private OpenShiftUtil openshift;

    @Test
    @Order(1)
    public void originalConfigMap_simpleInvocation() {
        when()
                .get("/hello")
                .then()
                .statusCode(200)
                .body("content", is("Hello World from Secret!"));
    }

    @Test
    @Order(2)
    public void originalConfigMap_parameterizedInvocation() {
        given()
                .queryParam("name", "Albert Einstein")
                .when()
                .get("/hello")
                .then()
                .statusCode(200)
                .body("content", is("Hello Albert Einstein from Secret!"));
    }

    @Test
    @Order(3)
    public void updatedConfigMap() throws Exception {
        openshift.applyYaml(new File("target/test-classes/secret-update.yaml"));
        openshift.rolloutChanges(metadata.appName);

        when()
                .get("/hello")
                .then()
                .statusCode(200)
                .body(containsString("Good morning World from an updated Secret!"));
    }

    @Test
    @Order(4)
    public void wrongConfigMap() throws Exception {
        openshift.applyYaml(new File("target/test-classes/secret-broken.yaml"));
        openshift.rolloutChanges(metadata.appName);

        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .get("/hello")
                    .then()
                    .statusCode(500);
        });
    }
}
