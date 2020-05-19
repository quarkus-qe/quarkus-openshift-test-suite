package io.quarkus.ts.openshift.todo.demo.app;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.ManualApplicationDeployment;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static io.restassured.RestAssured.when;

@OpenShiftTest
@ManualApplicationDeployment(appName = "todo-demo-app", httpRoot = "/", knownEndpoint = "/")
@AdditionalResources("classpath:openjdk-11-rhel7.yaml")
@AdditionalResources("classpath:todo-demo-app.yaml")
public class TodoDemoAppOpenShiftIT {
    @TestResource
    private URL url;

    @Test
    public void verify() {
        when()
                .get(url)
        .then()
                .statusCode(200);
    }
}
