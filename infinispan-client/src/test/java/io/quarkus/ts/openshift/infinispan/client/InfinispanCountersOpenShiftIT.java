package io.quarkus.ts.openshift.infinispan.client;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.OpenShiftTestException;
import io.quarkus.ts.openshift.common.deploy.UsingQuarkusPluginDeploymentStrategy;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@OpenShiftTest(strategy = UsingQuarkusPluginDeploymentStrategy.class)
@AdditionalResources("classpath:clientcert_secret.yaml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InfinispanCountersOpenShiftIT extends AbstractInfinispanResourceTest {

    @Test
    @Order(1)
    public void testConnectToEndpoints() {
        String firstEndpointCache = getCounterValue(appUrl + "/first-counter/get-cache");
        String secondEndpointCache = getCounterValue(appUrl + "/second-counter/get-cache");

        assertEquals(firstEndpointCache, secondEndpointCache);
    }

    @Test
    @Order(2)
    public void testUpdateCacheOnEndpoints() {
        String firstEndpointCounters = fillTheCache(appUrl + "/first-counter/increment-counters");
        String secondEndpointCounters = fillTheCache(appUrl + "/second-counter/increment-counters");

        assertEquals("Cache=1 Client=1", firstEndpointCounters);
        assertEquals("Cache=2 Client=1", secondEndpointCounters);
    }

    @Test
    @Order(3)
    public void testCacheAfterClientsRestart() {
        // always start from 0
        resetCacheCounter(appUrl + "/first-counter/reset-cache");
        resetClientCounter(appUrl + "/first-counter/reset-client");
        // fill the cache
        incrementCountersOnValue(appUrl + "/first-counter/increment-counters", 10);

        // restart the app
        openshift.rolloutChanges(metadata.appName);

        String cacheCounter = getCounterValue(appUrl + "/first-counter/get-cache");
        String clientCounter = getCounterValue(appUrl + "/first-counter/get-client");

        assertEquals("10", cacheCounter);
        assertEquals("0", clientCounter);
    }

    @Test
    @Order(4)
    public void testInvokeWithFailedNode() {
        resetCacheCounter(appUrl + "/first-counter/reset-cache");
        resetClientCounter(appUrl + "/first-counter/reset-client");

        incrementCountersOnValue(appUrl + "/first-counter/increment-counters", 10);

        // kill the app = fail of the client
        int replicas = openshift.countReadyReplicas(metadata.appName);
        openshift.scale(metadata.appName, 0);

        // try to invoke the cache
        when()
                .put(appUrl + "/first-counter/increment-counters")
                .then()
                .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(500), Matchers.lessThan(600)));

        // turn-on the app
        openshift.scale(metadata.appName, replicas);
        await.awaitAppRoute();

        String cacheCounter = getCounterValue(appUrl + "/first-counter/get-cache");
        String clientCounter = getCounterValue(appUrl + "/first-counter/get-client");

        assertEquals("10", cacheCounter);
        assertEquals("0", clientCounter);
    }

    @Test
    @Order(5)
    public void testRestartInfinispanCluster() throws IOException, InterruptedException {
        resetCacheCounter(appUrl + "/first-counter/reset-cache");
        resetClientCounter(appUrl + "/first-counter/reset-client");

        incrementCountersOnValue(appUrl + "/first-counter/increment-counters", 10);

        killInfinispanCluster();
        restartInfinispanCluster();

        // try to connect back to infinispan cluster and expect no content
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .get(appUrl + "/first-counter/get-cache")
            .then()
                    .statusCode(204);
        });

        String clientCounter = getCounterValue(appUrl + "/first-counter/get-client");
        assertEquals("10", clientCounter);
    }

    @Test
    @Order(6)
    public void testIncrementAfterRestartInfinispanCluster() throws IOException, InterruptedException {
        resetCacheCounter(appUrl + "/first-counter/reset-cache");
        resetClientCounter(appUrl + "/first-counter/reset-client");

        incrementCountersOnValue(appUrl + "/first-counter/increment-counters", 10);

        killInfinispanCluster();
        restartInfinispanCluster();

        // try to connect back to infinispan cluster and expect no content
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .get(appUrl + "/first-counter/get-cache")
                    .then()
                    .statusCode(204);
        });

        // create the deleted cache counter again
        String zeroCache = fillTheCache(appUrl + "/first-counter/reset-cache");
        // try to increment counters
        String firstEndpointCounters = fillTheCache(appUrl + "/first-counter/increment-counters");

        assertEquals("Cache=0", zeroCache);
        assertEquals("Cache=1 Client=11", firstEndpointCounters);
    }

    @Test
    @Order(7)
    public void testInvokeOnFailedInfinispanCluster() throws IOException, InterruptedException {
        resetCacheCounter(appUrl + "/first-counter/reset-cache");
        resetClientCounter(appUrl + "/first-counter/reset-client");

        incrementCountersOnValue(appUrl + "/first-counter/increment-counters", 10);

        killInfinispanCluster();

        // try to increment counters
        when()
                .put(appUrl + "/first-counter/increment-counters")
                .then()
                .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(500), Matchers.lessThan(600)));

        restartInfinispanCluster();

        // try to connect back to infinispan cluster and expect no content
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .get(appUrl + "/first-counter/get-cache")
                    .then()
                    .statusCode(204);
        });

        // check the client counter
        String clientCounter = getCounterValue(appUrl + "/first-counter/get-client");
        assertEquals("11", clientCounter);
    }

    @Test
    @Order(8)
    public void testConnectSecondClient() throws OpenShiftTestException {
        resetCacheCounter(appUrl + "/first-counter/reset-cache");

        String secondClientCache = getCounterValue(openshift.getUrlFromRoute(SECOND_CLIENT_APPLICATION_NAME) + "/first-counter/get-cache");
        assertEquals("0", secondClientCache);
    }

    @Test
    @Order(9)
    public void testMultipleClientIncrement() throws OpenShiftTestException {
        resetCacheCounter(appUrl + "/first-counter/reset-cache");
        resetClientCounter(appUrl + "/first-counter/reset-client");
        resetClientCounter(openshift.getUrlFromRoute(SECOND_CLIENT_APPLICATION_NAME) + "/first-counter/reset-client");

        // fill the cache in first and second client
        incrementCountersOnValue(appUrl + "/first-counter/increment-counters", 10);
        incrementCountersOnValue(openshift.getUrlFromRoute(SECOND_CLIENT_APPLICATION_NAME) + "/first-counter/increment-counters", 10);

        // save the cache counters in first and second client
        String firstClientCacheCounter = getCounterValue(appUrl + "/first-counter/get-cache");
        String secondClientCacheCounter = getCounterValue(openshift.getUrlFromRoute(SECOND_CLIENT_APPLICATION_NAME) + "/first-counter/get-cache");

        // save the client counters in first and second client
        String firstClientAppCounter = getCounterValue(appUrl + "/first-counter/get-client");
        String secondClientAppCounter = getCounterValue(openshift.getUrlFromRoute(SECOND_CLIENT_APPLICATION_NAME) + "/first-counter/get-client");

        assertEquals("10", firstClientAppCounter);
        assertEquals("10", secondClientAppCounter);
        assertEquals("20", firstClientCacheCounter);
        assertEquals("20", secondClientCacheCounter);
    }

    @Test
    @Order(10)
    public void testMultipleClientDataAfterRestartInfinispanCluster() throws IOException, InterruptedException, OpenShiftTestException {
        // reset the first client
        resetCacheCounter(appUrl + "/first-counter/reset-cache");
        resetClientCounter(appUrl + "/first-counter/reset-client");

        // reset the second client
        resetClientCounter(openshift.getUrlFromRoute(SECOND_CLIENT_APPLICATION_NAME) + "/first-counter/reset-client");

        // update the cache in both clients
        String firstClientCounters = fillTheCache(appUrl + "/first-counter/increment-counters");
        String secondClientCounters = fillTheCache(openshift.getUrlFromRoute(SECOND_CLIENT_APPLICATION_NAME) + "/first-counter/increment-counters");

        assertEquals("Cache=1 Client=1", firstClientCounters);
        assertEquals("Cache=2 Client=1", secondClientCounters);

        killInfinispanCluster();
        restartInfinispanCluster();

        // create the deleted cache counter again
        resetCacheCounter(appUrl + "/first-counter/reset-cache");

        // increment counters by the first and second client
        firstClientCounters = fillTheCache(appUrl + "/first-counter/increment-counters");
        secondClientCounters = fillTheCache(openshift.getUrlFromRoute(SECOND_CLIENT_APPLICATION_NAME) + "/first-counter/increment-counters");

        assertEquals("Cache=1 Client=2", firstClientCounters);
        assertEquals("Cache=2 Client=2", secondClientCounters);
    }
}
