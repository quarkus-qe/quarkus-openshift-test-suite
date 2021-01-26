package io.quarkus.ts.openshift.deployment.strategies.quarkus.serverless;

import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.deploy.UsingQuarkusPluginDeploymentStrategy;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@OpenShiftTest(strategy = UsingQuarkusPluginDeploymentStrategy.class)
public class QuarkusDeploymentStrategyOpenShiftServerlessIT {

    @Test
    public void hello() {
        when()
                .get("/hello")
        .then()
                .statusCode(200)
                .body("content", is("Hello, World!"));
    }
}
