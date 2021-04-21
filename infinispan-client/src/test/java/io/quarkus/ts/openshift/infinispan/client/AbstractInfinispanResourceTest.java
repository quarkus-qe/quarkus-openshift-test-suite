package io.quarkus.ts.openshift.infinispan.client;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.Command;
import io.quarkus.ts.openshift.common.CustomizeApplicationDeployment;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.AwaitUtil;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;
import org.junit.jupiter.api.AfterAll;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

public abstract class AbstractInfinispanResourceTest {
    protected static final String ORIGIN_CLUSTER_NAME = "totally-random-infinispan-cluster-name";
    protected static final String CLUSTER_CONFIG_PATH = "target/test-classes/infinispan_cluster_config.yaml";
    protected static final String CLUSTER_CONFIGMAP_PATH = "target/test-classes/infinispan_cluster_configmap.yaml";
    protected static final String CONNECT_SECRET = "target/test-classes/connect_secret.yaml";
    protected static final String TLS_SECRET = "target/test-classes/tls_secret.yaml";

    protected static final String CLUSTER_NAMESPACE_NAME = "datagrid-cluster";
    protected static final String SECOND_CLIENT_APPLICATION_NAME = "another-infinispan-client";
    protected static final String SECOND_CLIENT_DEPLOYMENT_CONFIG = "target/test-classes/deployment_config_second_client.yaml";
    protected static String NEW_CLUSTER_NAME = null;

    @TestResource
    protected AppMetadata metadata;

    @TestResource
    protected OpenShiftUtil openshift;

    @TestResource
    protected AwaitUtil await;

    @TestResource
    protected URL appUrl;

    /**
     * Application deployment is performed by the Quarkus Kubernetes extension during test execution.
     * Creating an infinispan cluster, its secrets, setting the path to it for the application, and deploying the second app.
     *
     * @param oc
     * @param metadata
     * @throws IOException
     * @throws InterruptedException
     */
    @CustomizeApplicationDeployment
    public static void deploy(OpenShiftClient oc, AppMetadata metadata) throws IOException, InterruptedException {
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

        deploySecondInfinispanClient(oc, metadata);
    }

    // Undeployment of the second application and infinispan cluster
    @AfterAll
    public static void undeploy() throws IOException, InterruptedException {
        new Command("oc", "delete", "-f", SECOND_CLIENT_DEPLOYMENT_CONFIG).runAndWait();
        new Command("oc", "delete", "-f", CLUSTER_CONFIGMAP_PATH).runAndWait();
        new Command("oc", "delete", "-f", CLUSTER_CONFIG_PATH).runAndWait();
    }

    /**
     * This method copy the 'openshift.yml' file, changes its name, labels, etc., and deploys it as a second application in OCP.
     * For that we need only DeploymentConfig, Service, and Route.
     *
     * @param oc
     * @param metadata
     * @throws IOException
     * @throws InterruptedException
     */
    public static void deploySecondInfinispanClient(OpenShiftClient oc, AppMetadata metadata) throws IOException, InterruptedException {
        List<HasMetadata> objs = oc.load(Files.newInputStream(Paths.get("target/kubernetes/openshift.yml"))).get();
        List<HasMetadata> necessary_objects = new ArrayList<>();

        HashMap<String, String> change = new HashMap<>();
        change.put("app.kubernetes.io/name", SECOND_CLIENT_APPLICATION_NAME);

        for (HasMetadata obj : objs) {
            if (obj.getMetadata().getName().equals(metadata.appName)) {
                if (obj instanceof DeploymentConfig) {
                    DeploymentConfig dc = (DeploymentConfig) obj;
                    dc.getMetadata().setName(SECOND_CLIENT_APPLICATION_NAME);
                    dc.getMetadata().setLabels(change);
                    dc.getSpec().setSelector(change);
                    dc.getSpec().getTemplate().getMetadata().setLabels(change);
                    necessary_objects.add(dc);
                }

                if (obj instanceof Service) {
                    Service service = (Service) obj;
                    service.getMetadata().setName(SECOND_CLIENT_APPLICATION_NAME);
                    service.getSpec().setSelector(change);
                    necessary_objects.add(service);
                }

                if (obj instanceof Route) {
                    Route route = (Route) obj;
                    route.getMetadata().setName(SECOND_CLIENT_APPLICATION_NAME);
                    route.getSpec().getTo().setName(SECOND_CLIENT_APPLICATION_NAME);
                    route.getSpec().setHost("");
                    route.getSpec().setPath("");
                    necessary_objects.add(route);
                }
            }
        }

        KubernetesList list = new KubernetesList();
        list.setItems(necessary_objects);
        Serialization.yamlMapper().writeValue(Files.newOutputStream(Paths.get(new File(SECOND_CLIENT_DEPLOYMENT_CONFIG).getPath())), list);

        new Command("oc", "apply", "-f", SECOND_CLIENT_DEPLOYMENT_CONFIG).runAndWait();
    }

    /**
     * Setting the cache counter value to 0 from provided client url address.
     * At the end, the cache value is tested that it is actually 0.
     *
     * @param url
     */
    public void resetCacheCounter(String url) {
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .put(url)
                    .then()
                    .body(is("Cache=0"));
        });
    }

    /**
     * Setting the client atomic integer counter to 0 in the provided client url address.
     * At the end, the client counter value is tested that it is actually 0.
     *
     * @param url
     */
    public void resetClientCounter(String url) {
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .put(url)
                    .then()
                    .body(is("Client=0"));
        });
    }

    /**
     * Getting the value of either cache or client counters from the provided url address.
     * Tested is only the right returned status code.
     *
     * @param url
     * @return endpoint value as String
     */
    public String getCounterValue(String url) {
        String actualResponse =
            when()
                    .get(url)
                    .then().statusCode(200)
                    .extract().asString();

        return actualResponse;
    }

    /**
     * Increasing cache and client counters by 1 from the provided url address.
     *
     * @param url
     * @return increased endpoint value as String
     */
    public String fillTheCache(String url) {
        String actualResponse =
            when()
                    .put(url)
                    .then().statusCode(200)
                    .extract().asString();

        return actualResponse;
    }

    /**
     * Increasing cache and client counters by the provided count value from the provided url address.
     *
     * @param url
     * @param count
     */
    public void incrementCountersOnValue(String url, int count) {
        for (int i = 1; i <= count; i++) {
            when()
                    .put(url)
                    .then()
                    .statusCode(200);
        }
    }

    /**
     * Reduces the number of infinispan cluster replicas to 0 and wait for the shutdown condition. It is done by changing
     * the YAML file in the target/test-classes directory.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void killInfinispanCluster() throws IOException, InterruptedException {
        adjustYml(CLUSTER_CONFIG_PATH, "replicas: 1", "replicas: 0");
        new Command("oc", "apply", "-f", CLUSTER_CONFIG_PATH).runAndWait();
        new Command("oc", "-n", CLUSTER_NAMESPACE_NAME, "wait", "--for", "condition=gracefulShutdown", "--timeout=300s", "infinispan/" + NEW_CLUSTER_NAME).runAndWait();
    }

    /**
     * The number of replicas is increased back to value 1 the same way as in "killInfinispanCluster()" method. The wait command
     * expects "wellFormed" condition in Infinispan cluster status.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void restartInfinispanCluster() throws IOException, InterruptedException {
        adjustYml(CLUSTER_CONFIG_PATH, "replicas: 0", "replicas: 1");
        new Command("oc", "apply", "-f", CLUSTER_CONFIG_PATH).runAndWait();
        new Command("oc", "-n", CLUSTER_NAMESPACE_NAME, "wait", "--for", "condition=wellFormed", "--timeout=360s", "infinispan/" + NEW_CLUSTER_NAME).runAndWait();
    }

    /**
     * Replacing values in the provided YAML file
     *
     * @param path
     * @param originString
     * @param newString
     * @throws IOException
     */
    public static void adjustYml(String path, String originString, String newString) throws IOException {
        Path yamlPath = Paths.get(path);
        Charset charset = StandardCharsets.UTF_8;

        String yamlContent = new String(Files.readAllBytes(yamlPath), charset);
        yamlContent = yamlContent.replace(originString, newString);
        Files.write(yamlPath, yamlContent.getBytes(charset));
    }
}
