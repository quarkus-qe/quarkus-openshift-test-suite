package io.quarkus.ts.openshift.infinispan.client;

import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.Command;
import io.quarkus.ts.openshift.common.CustomizeApplicationDeployment;
import io.quarkus.ts.openshift.common.CustomizeApplicationUndeployment;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.AwaitUtil;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;
import org.junit.jupiter.api.AfterAll;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

public abstract class AbstractInfinispanResourceTest {
    protected static final String ORIGIN_CLUSTER_NAME = "totally-random-infinispan-cluster-name";
    protected static final String CLUSTER_CONFIG_PATH = "target/test-classes/infinispan_cluster_config.yaml";
    protected static final String CLUSTER_CONFIGMAP_PATH = "target/test-classes/infinispan_cluster_configmap.yaml";
    protected static final String CONNECT_SECRET = "target/test-classes/connect_secret.yaml";
    protected static final String TLS_SECRET = "target/test-classes/tls_secret.yaml";
    protected static final String DEPLOYMENT_CONFIG_SECOND_CLIENT = "target/test-classes/deployment_config_infinispan_client.yaml";

    protected static final String CLUSTER_NAMESPACE_NAME = "datagrid-cluster";
    protected static final String SECOND_CLIENT_APPLICATION_NAME = "another-infinispan-client";
    protected static String NEW_CLUSTER_NAME = null;

    @TestResource
    protected AppMetadata metadata;

    @TestResource
    protected OpenShiftUtil openshift;

    @TestResource
    protected AwaitUtil await;

    @TestResource
    protected URL appUrl;

    // Application deployment is performed by the Quarkus Kubernetes extension during test execution.
    // Creating an infinispan cluster, its secrets and setting the path to it for the application
    @CustomizeApplicationDeployment
    public static void deploy(OpenShiftClient oc) throws IOException, InterruptedException {
        new Command("oc", "apply", "-f", DEPLOYMENT_CONFIG_SECOND_CLIENT).runAndWait();

        new Command("oc", "apply", "-f", CONNECT_SECRET).runAndWait();
        new Command("oc", "apply", "-f", TLS_SECRET).runAndWait();

        // there should be unique name for every created infinispan cluster to be able parallel runs
        NEW_CLUSTER_NAME = oc.getNamespace() + "-infinispan-cluster";

        // rename infinispan cluster and configmap
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

    @AfterAll
    public static void deleteSecondClient() throws IOException, InterruptedException {
        new Command("oc", "delete", "-f", DEPLOYMENT_CONFIG_SECOND_CLIENT).runAndWait();
    }

    // setting the cache to 0
    public void resetCacheCounter(String url) {
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .put(url)
                    .then()
                    .body(is("Cache=0"));
        });
    }

    // setting the client counter to 0
    public void resetClientCounter(String url) {
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .put(url)
                    .then()
                    .body(is("Client=0"));
        });
    }

    // getting the value of either cache or client counters
    public String getCounterValue(String url) {
        String actualResponse = given()
                .when()
                .get(url)
                .then().statusCode(200)
                .extract().asString();

        return actualResponse;
    }

    // increasing cache and client counters by 1
    public String fillTheCache(String url) {
        String actualResponse = given()
                .when()
                .put(url)
                .then().statusCode(200)
                .extract().asString();

        return actualResponse;
    }

    public void incrementCountersOnValue(String url, int count) {
        for (int i = 1; i <= count; i++) {
            given().put(url).print();
        }
    }

    // reduces a number of infinispan cluster replicas to 0 and wait for the shutdown condition
    public void killInfinispanCluster() throws IOException, InterruptedException {
        adjustYml(CLUSTER_CONFIG_PATH, "replicas: 1", "replicas: 0");
        new Command("oc", "apply", "-f", CLUSTER_CONFIG_PATH).runAndWait();
        new Command("oc", "-n", CLUSTER_NAMESPACE_NAME, "wait", "--for", "condition=gracefulShutdown", "--timeout=300s", "infinispan/" + NEW_CLUSTER_NAME).runAndWait();
    }

    public void restartInfinispanCluster() throws IOException, InterruptedException {
        adjustYml(CLUSTER_CONFIG_PATH, "replicas: 0", "replicas: 1");
        new Command("oc", "apply", "-f", CLUSTER_CONFIG_PATH).runAndWait();
        new Command("oc", "-n", CLUSTER_NAMESPACE_NAME, "wait", "--for", "condition=wellFormed", "--timeout=300s", "infinispan/" + NEW_CLUSTER_NAME).runAndWait();
    }

    public static void adjustYml(String path, String originString, String newString) throws IOException {
        Path yamlPath = Paths.get(path);
        Charset charset = StandardCharsets.UTF_8;

        String yamlContent = new String(Files.readAllBytes(yamlPath), charset);
        yamlContent = yamlContent.replace(originString, newString);
        Files.write(yamlPath, yamlContent.getBytes(charset));
    }
}
