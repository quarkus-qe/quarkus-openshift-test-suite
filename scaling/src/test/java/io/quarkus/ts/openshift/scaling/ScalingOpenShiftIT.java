package io.quarkus.ts.openshift.scaling;

import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;
import static org.junit.jupiter.api.Assertions.assertEquals;

@OpenShiftTest
public class ScalingOpenShiftIT {

    public static final String DC_NAME = "test-scaling";
    private static final int TIMEOUT_SEC = 60;
    private static final int DELAY_BETWEEN_REQUEST_MS = 100;
    private static final int POLL_INTERVAL_MS = 1000;
    private static final int READINESS_TIMEOUT_MIN = 3;

    @TestResource
    private OpenShiftUtil openShiftUtil;

    @AfterEach
    public void scaleBack() {
        openShiftUtil.scale(DC_NAME, 1);
    }

    /**
     * Workflow:
     * * Make sure the single replica is running.
     * * Scale up to two replicas.
     * * Wait for their readiness and verify that both of the replicas are responding.
     */
    @Test
    public void scaleUpTest() {
        int REPLICAS = 2;

        givenResourcePath("/scaling");
        whenScaleTo(2);
        thenCheckReplicasAmount(2);
        whenMakeRequestTo("/scaling", REPLICAS);
    }


    /**
     * Workflow:
     * * Scale to two replicas and verify that both of the replicas are responding.
     * * Scale down to a single replica.
     * * Execute an arbitrary minimal sample of requests and verify that all are served by the same replica.
     */
    @Test
    public void scaleDownTest() throws InterruptedException {

        int REPLICAS = 2;
        givenResourcePath("/scaling");
        whenScaleTo(2);
        thenCheckReplicasAmount(2);
        whenMakeRequestTo("/scaling", REPLICAS);

        REPLICAS = 1;
        whenScaleTo(1);
        thenCheckReplicasAmount(1);
        whenMakeRequestTo("/scaling", REPLICAS);
    }


    /**
     * Workflow:
     * * Scale down to zero replicas.
     * * Execute an arbitrary minimal sample of requests and verify that all get HTTP 503 response.
     */
    @Test
    public void scaleToZero() throws InterruptedException {

        givenResourcePath("/scaling");
        whenScaleTo(0);
        thenCheckReplicasAmount(0);
        makeHttpScalingRequest("/scaling", SERVICE_UNAVAILABLE.getStatusCode());
    }

    private void givenResourcePath(String path) {
        with().pollInterval(Duration.ofMillis(POLL_INTERVAL_MS))
                .await().atMost(TIMEOUT_SEC, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    given()
                            .when().get("/scaling")
                            .then()
                            .log().body()
                            .log().status()
                            .statusCode(OK.getStatusCode());
                });
    }

    private void whenScaleTo(int amount) {
        openShiftUtil.scale(DC_NAME, amount, READINESS_TIMEOUT_MIN, TimeUnit.MINUTES);
    }

    private void thenCheckReplicasAmount(int expectedAmount) {
        assertThat(openShiftUtil.countReadyReplicas(DC_NAME)).isEqualTo(expectedAmount);
    }

    private void whenMakeRequestTo(String path, int expectedReplicas) {
        Set<String> replicas = new HashSet<>();
        await().pollInterval(DELAY_BETWEEN_REQUEST_MS, TimeUnit.MILLISECONDS)
                .atMost(TIMEOUT_SEC, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    replicas.add(makeHttpScalingRequest(path, OK.getStatusCode()).extract().asString());
                    assertEquals(expectedReplicas, replicas.size());
                });
    }

    private ValidatableResponse makeHttpScalingRequest(String path, int expectedHttpStatus){
        return given()
                .when().get(path)
                .then()
                .statusCode(expectedHttpStatus);
    }

}
