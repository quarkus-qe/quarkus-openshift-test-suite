package io.quarkus.ts.openshift.infinispan.client;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OnlyIfConfigured;
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
@OnlyIfConfigured("ts.authenticated-registry")
public class InfinispanCountersOpenShiftIT extends AbstractInfinispanResourceTest {

    /**
     * Simple check of connection to endpoints
     *
     * Expected values = 0
     */
    @Test
    @Order(1)
    public void testConnectToEndpoints() {
        String firstEndpointCache = getCounterValue(appUrl + "/first-counter/get-cache");
        String secondEndpointCache = getCounterValue(appUrl + "/second-counter/get-cache");

        assertEquals(firstEndpointCache, secondEndpointCache);
    }

    /**
     * Test increment counters by 1
     *
     * Client counters should be 1 for both endpoints
     * Cache counter is shared and should be 2
     */
    @Test
    @Order(2)
    public void testUpdateCacheOnEndpoints() {
        String firstEndpointCounters = fillTheCache(appUrl + "/first-counter/increment-counters");
        String secondEndpointCounters = fillTheCache(appUrl + "/second-counter/increment-counters");

        assertEquals("Cache=1 Client=1", firstEndpointCounters);
        assertEquals("Cache=2 Client=1", secondEndpointCounters);
    }

    /**
     * Client fail-over test. Testing the Quarkus application will connect back to the DataGrid server after restart.
     *
     * Cache counter should remain the same.
     * Client counter is reset to 0
     */
    @Test
    @Order(3)
    public void testCacheAfterClientsRestart() {
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

    /**
     * Client fail-over test. Testing the request to the DataGrid server by the failed Quarkus application.
     *
     * Cache counter should remain the same.
     * Client counter is reset to 0
     */
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

    /**
     * Infinispan fail-over test. Testing restart the infinispan cluster in DataGrid operator and wait the Quarkus
     * application connects back. The restart is done by reducing the number of infinispan cluster replicas to 0 and it waits
     * for the shutdown condition. Then the number of replicas is changed back to 1.
     *
     * We don't have cache backup in this test case, so the cache is deleted by the restart of infinispan cluster.
     * The cache definition "mycache" remains, but the "counter" cache in it is deleted.
     * Client counter should remain with the same value after the restart.
     *
     * @throws IOException
     * @throws InterruptedException
     */
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

    /**
     * Infinispan fail-over test. Testing a restart of the infinispan cluster and increment/change the cache counter value
     * after the restart. The cache is deleted by the restart of infinispan cluster. Because of this, we need to fill the cache
     * again. It is done by 'cache.put("counter", 0)'. Then it could be incremented.
     *
     * Cache newly created after the restart and incremented by 1 so it should be only 1.
     * Client counter should remain the same during the restart and after the counter incrementing should by increased by 1.
     *
     * @throws IOException
     * @throws InterruptedException
     */
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

    /**
     * Infinispan fail-over test. Test invoke a request on the Infinispan server which is currently down.
     * Because of our settings in the hotrod-client.properties file, the application is trying to connect only once and only 1s.
     * By default, the app is trying to connect 60 s with 10 retries even when the next tests continue. It means that the counter
     * could be unexpectedly increased in one of the next tests
     *
     * Cache should be empty (status code 204).
     * Client counter should be increased even if the server is down.
     *
     * @throws IOException
     * @throws InterruptedException
     */
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

    /**
     * Check the connection to the second client (second Quarkus application).
     *
     * @throws OpenShiftTestException
     */
    @Test
    @Order(8)
    public void testConnectSecondClient() throws OpenShiftTestException {
        resetCacheCounter(appUrl + "/first-counter/reset-cache");

        String secondClientCache = getCounterValue(openshift.getUrlFromRoute(SECOND_CLIENT_APPLICATION_NAME) + "/first-counter/get-cache");
        assertEquals("0", secondClientCache);
    }

    /**
     * Testing the cache is shared between clients (apps). Every client has its own client counter.
     *
     * Clients counters should be increased only if the increase is called by their client.
     * Cache counter is shared and should contain the sum of both client counters.
     *
     * @throws OpenShiftTestException
     */
    @Test
    @Order(9)
    public void testMultipleClientIncrement() throws OpenShiftTestException {
        // reset the first and client
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

        // sum of both client counters
        String cacheValue = String.valueOf(Integer.valueOf(firstClientAppCounter) + Integer.valueOf(secondClientAppCounter));
        assertEquals(cacheValue, firstClientCacheCounter);
        assertEquals(cacheValue, secondClientCacheCounter);
    }

    /**
     * Multiple client Infinispan fail-over test. Testing restart the infinispan cluster and increment/change counters values
     * of both client applications after the restart.
     *
     * Cache newly created after the restart and incremented by 1 by each client so it should by on value 2.
     * Client counters should remain the same during the restart and after the counters incrementing both are increased by 1.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws OpenShiftTestException
     */
    @Test
    @Order(10)
    public void testMultipleClientDataAfterRestartInfinispanCluster() throws IOException, InterruptedException, OpenShiftTestException {
        resetCacheCounter(appUrl + "/first-counter/reset-cache");
        resetClientCounter(appUrl + "/first-counter/reset-client");
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
