package io.quarkus.ts.openshift.infinispan.client;

import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.Command;
import io.quarkus.ts.openshift.common.CustomizeApplicationDeployment;
import io.quarkus.ts.openshift.common.CustomizeApplicationUndeployment;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.deploy.UsingQuarkusPluginDeploymentStrategy;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.AwaitUtil;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;
import org.hamcrest.Matchers;
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
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

@OpenShiftTest(strategy = UsingQuarkusPluginDeploymentStrategy.class)
@AdditionalResources("classpath:clientcert_secret.yaml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InfinispanCountersOpenShiftIT {

    private static final String ORIGIN_CLUSTER_NAME = "totally-random-infinispan-cluster-name";
    private static final String CLUSTER_CONFIG_PATH = "target/test-classes/infinispan_cluster_config.yaml";
    private static final String CLUSTER_CONFIGMAP_PATH = "target/test-classes/infinispan_cluster_configmap.yaml";

    private static final String CLUSTER_NAMESPACE_NAME = "datagrid-cluster";
    private static String NEW_CLUSTER_NAME = null;

    @TestResource
    private AppMetadata metadata;

    @TestResource
    private OpenShiftUtil openshift;

    @TestResource
    private AwaitUtil await;

    // Application deployment is performed by the Quarkus Kubernetes extension during test execution.
    // Creating an infinispan cluster, its secrets and setting the path to it for the application
    @CustomizeApplicationDeployment
    public static void deploy(OpenShiftClient oc) throws IOException, InterruptedException {
        new Command("oc", "apply", "-f", "target/test-classes/connect_secret.yaml").runAndWait();
        new Command("oc", "apply", "-f", "target/test-classes/tls_secret.yaml").runAndWait();

        // there should be unique name for every created infinispan cluster to be able parallel runs
        NEW_CLUSTER_NAME = oc.getNamespace() + "-infinispan-cluster";

        adjustYml(CLUSTER_CONFIG_PATH, ORIGIN_CLUSTER_NAME, NEW_CLUSTER_NAME);
        adjustYml(CLUSTER_CONFIGMAP_PATH, ORIGIN_CLUSTER_NAME, NEW_CLUSTER_NAME);

        new Command("oc", "apply", "-f", CLUSTER_CONFIGMAP_PATH).runAndWait();
        new Command("oc", "apply", "-f", CLUSTER_CONFIG_PATH).runAndWait();

        new Command("oc", "-n", CLUSTER_NAMESPACE_NAME, "wait", "--for", "condition=wellFormed", "--timeout=300s", "infinispan/" + NEW_CLUSTER_NAME).runAndWait();
    }

    // Undeployment of the application and infinispan cluster
    @CustomizeApplicationUndeployment
    public static void undeploy() throws IOException, InterruptedException {
        new Command("oc", "delete", "-f", CLUSTER_CONFIGMAP_PATH).runAndWait();
        new Command("oc", "delete", "-f", CLUSTER_CONFIG_PATH).runAndWait();
    }

    @Test
    @Order(1)
    public void testConnectToEndpoints() {
        when()
                .get("/first-counter/get-cache")
                .then()
                .statusCode(200)
                .body(is("0"));

        when()
                .get("/second-counter/get-cache")
                .then()
                .statusCode(200)
                .body(is("0"));
    }

    @Test
    @Order(2)
    public void testUpdateCacheOnEndpoints() {
        // fill the cache in the first class
        when()
                .put("/first-counter/increment-counters")
                .then()
                .statusCode(200)
                .body(is("Cache=1 Client=1"));

        // fill the cache in the second class
        when()
                .put("/second-counter/increment-counters")
                .then()
                .statusCode(200)
                .body(is("Cache=2 Client=1"));

        // check the cache counter
        when()
                .get("/first-counter/get-cache")
                .then()
                .statusCode(200)
                .body(is("2"));

        when()
                .get("/second-counter/get-cache")
                .then()
                .statusCode(200)
                .body(is("2"));

        // check the client counter
        when()
                .get("/first-counter/get-client")
                .then()
                .statusCode(200)
                .body(is("1"));

        when()
                .get("/second-counter/get-client")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    @Order(3)
    public void testCacheAfterClientsRestart() {
        // always start from 0
        resetCounters();
        // fill the cache
        incrementCountersOnValue(10);

        // restart the app
        openshift.rolloutChanges(metadata.appName);

        // check the cache counter
        when()
                .get("/first-counter/get-cache")
                .then()
                .statusCode(200)
                .body(is("10"));

        // check the client counter
        when()
                .get("/first-counter/get-client")
                .then()
                .statusCode(200)
                .body(is("0"));
    }

    @Test
    @Order(4)
    public void testInvokeWithFailedNode() {
        resetCounters();
        incrementCountersOnValue(10);

        // kill the app = fail of the client
        int replicas = openshift.countReadyReplicas(metadata.appName);
        openshift.scale(metadata.appName, 0);

        // try to invoke the cache
        when()
                .put("/first-counter/increment-counters")
                .then()
                .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(500),Matchers.lessThan(600)));

        // turn-on the app
        openshift.scale(metadata.appName, replicas);
        await.awaitAppRoute();

        // check the cache counter
        when()
                .get("/first-counter/get-cache")
                .then()
                .statusCode(200)
                .body(is("10"));

        // check the client counter
        when()
                .get("/first-counter/get-client")
                .then()
                .statusCode(200)
                .body(is("0"));
    }

    @Test
    @Order(5)
    public void testRestartInfinispanCluster() throws IOException, InterruptedException {
        resetCounters();
        incrementCountersOnValue(10);

        killInfinispanCluster();
        restartInfinispanCluster();

        // try to connect back to infinispan cluster and expect no content
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .get("/first-counter/get-cache")
            .then()
                    .statusCode(204);
        });

        // check the client counter
        when()
                .get("/first-counter/get-client")
                .then()
                .body(is("10"));
    }

    @Test
    @Order(6)
    public void testIncrementAfterRestartInfinispanCluster() throws IOException, InterruptedException {
        resetCounters();
        incrementCountersOnValue(10);

        killInfinispanCluster();
        restartInfinispanCluster();

        // try to connect back to infinispan cluster and expect no content
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .get("/first-counter/get-cache")
                    .then()
                    .statusCode(204);
        });

        // create the deleted cache counter again
        when()
                .put("/first-counter/reset-cache")
                .then()
                .statusCode(200)
                .body(is("Cache=0"));

        // try to increment counters
        when()
                .put("/first-counter/increment-counters")
                .then()
                .statusCode(200)
                .body(is("Cache=1 Client=11"));
    }

    @Test
    @Order(7)
    public void testInvokeOnFailedInfinispanCluster() throws IOException, InterruptedException {
        resetCounters();
        incrementCountersOnValue(10);

        killInfinispanCluster();

        // try to increment counters
        when()
                .put("/first-counter/increment-counters")
                .then()
                .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(500),Matchers.lessThan(600)));

        restartInfinispanCluster();

        // try to connect back to infinispan cluster and expect no content
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .get("/first-counter/get-cache")
                    .then()
                    .statusCode(204);
        });

        // check the client counter
        when()
                .get("/first-counter/get-client")
                .then()
                .body(is("11"));
    }

    private void resetCounters() {
        when()
                .put("/first-counter/reset-cache")
                .then()
                .body(is("Cache=0"));

        when()
                .put("/first-counter/reset-client")
                .then()
                .body(is("Client=0"));
    }

    private void incrementCountersOnValue(int count) {
        for (int i = 1; i <= count; i++) {
            when()
                    .put("/first-counter/increment-counters")
                    .then()
                    .body(is("Cache=" + i + " Client=" + i));
        }
    }

    private void killInfinispanCluster() throws IOException, InterruptedException {
        adjustYml(CLUSTER_CONFIG_PATH, "replicas: 1", "replicas: 0");
        new Command("oc", "apply", "-f", CLUSTER_CONFIG_PATH).runAndWait();
        new Command("oc", "-n", CLUSTER_NAMESPACE_NAME, "wait", "--for", "condition=gracefulShutdown", "--timeout=300s", "infinispan/" + NEW_CLUSTER_NAME).runAndWait();
    }

    private void restartInfinispanCluster() throws IOException, InterruptedException {
        adjustYml(CLUSTER_CONFIG_PATH, "replicas: 0", "replicas: 1");
        new Command("oc", "apply", "-f", CLUSTER_CONFIG_PATH).runAndWait();
        new Command("oc", "-n", CLUSTER_NAMESPACE_NAME, "wait", "--for", "condition=wellFormed", "--timeout=300s", "infinispan/" + NEW_CLUSTER_NAME).runAndWait();
    }

    private static void adjustYml(String path, String originString, String newString) throws IOException {
        Path yamlPath = Paths.get(path);
        Charset charset = StandardCharsets.UTF_8;

        String yamlContent = new String(Files.readAllBytes(yamlPath), charset);
        yamlContent = yamlContent.replace(originString, newString);
        Files.write(yamlPath, yamlContent.getBytes(charset));
    }
}
