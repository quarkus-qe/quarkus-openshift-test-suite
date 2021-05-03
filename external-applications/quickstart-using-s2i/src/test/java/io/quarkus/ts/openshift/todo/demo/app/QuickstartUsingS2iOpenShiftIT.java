package io.quarkus.ts.openshift.todo.demo.app;

import static io.restassured.RestAssured.when;

import java.net.URL;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.CustomAppMetadata;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.deploy.ManualDeploymentStrategy;
import io.quarkus.ts.openshift.common.injection.TestResource;

@OpenShiftTest(strategy = ManualDeploymentStrategy.class)
@CustomAppMetadata(appName = "quickstart-using-s2i", httpRoot = "/", knownEndpoint = "/")
@AdditionalResources("classpath:deployments/maven/s2i-maven-settings.yaml")
@AdditionalResources("classpath:quickstart-using-s2i.yaml")
public class QuickstartUsingS2iOpenShiftIT {
    @TestResource
    private URL url;

    @Test
    public void verify() {
        when()
                .get(url)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }
}
