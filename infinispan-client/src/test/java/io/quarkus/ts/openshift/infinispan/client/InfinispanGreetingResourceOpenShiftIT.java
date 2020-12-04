package io.quarkus.ts.openshift.infinispan.client;

import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.Command;
import io.quarkus.ts.openshift.common.CustomizeApplicationDeployment;
import io.quarkus.ts.openshift.common.CustomizeApplicationUndeployment;
import io.quarkus.ts.openshift.common.ManualApplicationDeployment;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

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
@AdditionalResources("classpath:clientcert_secret.yaml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InfinispanGreetingResourceOpenShiftIT {

    private static final String ORIGIN_CLUSTER_NAME = "totally-random-infinispan-cluster-name";
    private static final String CLUSTER_CONFIG_PATH = "target/test-classes/infinispan_cluster_config.yaml";
    private static final String CLUSTER_CONFIGMAP_PATH = "target/test-classes/infinispan_cluster_configmap.yaml";

    // Application deployment is performed by the Quarkus Kubernetes extension during application build,
    // because we set quarkus.kubernetes.deploy=true (see the appplication.properties).
    // Creating an infinispan cluster, its secrets and setting the path to it for the application
    @CustomizeApplicationDeployment
    public static void deploy(OpenShiftClient oc) throws IOException, InterruptedException {
        new Command("oc", "apply", "-f", "target/test-classes/connect_secret.yaml").runAndWait();
        new Command("oc", "apply", "-f", "target/test-classes/tls_secret.yaml").runAndWait();

        // there should be unique name for every created infinispan cluster to be able parallel runs
        String newClusterName = oc.getNamespace() + "-infinispan-cluster";

        adjustYml(CLUSTER_CONFIG_PATH, ORIGIN_CLUSTER_NAME, newClusterName);
        adjustYml(CLUSTER_CONFIGMAP_PATH, ORIGIN_CLUSTER_NAME, newClusterName);

        new Command("oc", "apply", "-f", CLUSTER_CONFIGMAP_PATH).runAndWait();
        new Command("oc", "apply", "-f", CLUSTER_CONFIG_PATH).runAndWait();
    }

    // Undeployment of the application and infinispan cluster
    @CustomizeApplicationUndeployment
    public static void undeploy() throws IOException, InterruptedException {
        new Command("oc", "delete", "-f", CLUSTER_CONFIGMAP_PATH).runAndWait();
        new Command("oc", "delete", "-f", CLUSTER_CONFIG_PATH).runAndWait();
        new Command("oc", "delete", "-f", "target/kubernetes/openshift.yml").runAndWait();
    }

    @Test
    @Order(1)
    public void testHelloFirstEndpoint() {
        when()
                .get("/first-endpoint")
                .then()
                .statusCode(200)
                .body(is("Hello World, Infinispan is up!"));
    }

    @Test
    @Order(2)
    public void testHelloSecondEndpoint() {
        when()
                .post("/first-endpoint")
                .then()
                .statusCode(200);

        when()
                .get("/second-endpoint")
                .then()
                .statusCode(200)
                .body(is("Hello World, Infinispan is up and changed!"));
    }

    private static void adjustYml(String path, String originString, String newString) throws IOException {
        Path yamlPath = Paths.get(path);
        Charset charset = StandardCharsets.UTF_8;

        String yamlContent = new String(Files.readAllBytes(yamlPath), charset);
        yamlContent = yamlContent.replace(originString, newString);
        Files.write(yamlPath, yamlContent.getBytes(charset));
    }

}
