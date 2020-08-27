package io.quarkus.ts.openshift.deployment.strategies.quarkus.serverless;

import io.fabric8.knative.client.KnativeClient;
import io.quarkus.ts.openshift.common.Command;
import io.quarkus.ts.openshift.common.CustomizeApplicationDeployment;
import io.quarkus.ts.openshift.common.CustomizeApplicationUndeployment;
import io.quarkus.ts.openshift.common.ManualApplicationDeployment;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@OpenShiftTest
@ManualApplicationDeployment
public class QuarkusDeploymentStrategyOpenShiftServerlessIT {

    // S2I build is performed during application build, see quarkus.container-image.build property in the POM
    // Deploy of the service is done as part of the test because current namespace needs to be reflected in the image name

    @CustomizeApplicationDeployment
    public static void deploy(KnativeClient kn) throws IOException, InterruptedException {
        adjustKnativeYml(kn.getNamespace());
        new Command("oc", "apply", "-f", "target/kubernetes/knative.yml").runAndWait();
    }

    @CustomizeApplicationUndeployment
    public static void undeploy() throws IOException, InterruptedException {
        new Command("oc", "delete", "-f", "target/kubernetes/knative.yml").runAndWait();
    }

    @Test
    public void hello() {
        when()
                .get("/hello")
        .then()
                .statusCode(200)
                .body("content", is("Hello, World!"));
    }

    private static void adjustKnativeYml(String currentNamespace) throws IOException {
        Path yamlPath = Paths.get("target/kubernetes/knative.yml");
        Charset charset = StandardCharsets.UTF_8;

        String yamlContent = new String(Files.readAllBytes(yamlPath), charset);
        yamlContent = yamlContent.replaceAll("to-be-replaced-by-current-namespace", currentNamespace);
        Files.write(yamlPath, yamlContent.getBytes(charset));
    }
}
