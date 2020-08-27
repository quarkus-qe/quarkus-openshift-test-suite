package io.quarkus.ts.openshift.deployment.strategies.quarkus;

import io.quarkus.ts.openshift.common.Command;
import io.quarkus.ts.openshift.common.CustomAppMetadata;
import io.quarkus.ts.openshift.common.CustomizeApplicationUndeployment;
import io.quarkus.ts.openshift.common.ManualApplicationDeployment;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@OpenShiftTest
@ManualApplicationDeployment
@CustomAppMetadata(appName = "deployment-strategy-quarkus", httpRoot = "/", knownEndpoint = "/health/ready")
public class QuarkusDeploymentStrategyOpenShiftIT {
    // deployment is performed by the Quarkus Kubernetes extension during application build,
    // because we set quarkus.kubernetes.deploy=true (see the POM)

    @CustomizeApplicationUndeployment
    public static void undeploy() throws IOException, InterruptedException {
        new Command("oc", "delete", "-f", "target/kubernetes/openshift.yml").runAndWait();
    }

    @Test
    public void hello() {
        when()
                .get("/hello")
        .then()
                .statusCode(200)
                .body("content", is("Hello, World!"));
    }
}
