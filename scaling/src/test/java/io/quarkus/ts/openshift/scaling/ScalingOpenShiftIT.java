package io.quarkus.ts.openshift.scaling;

import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@OpenShiftTest
public class ScalingOpenShiftIT {

    public static final String DC_NAME = "test-scaling";

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
        given()
                .when().get("/scaling")
                .then()
                .statusCode(OK.getStatusCode());

        openShiftUtil.scale(DC_NAME, 2);

        assertThat(openShiftUtil.countReadyReplicas(DC_NAME)).isEqualTo(2);

        Set<String> uniqueIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            String uniqueId =
                    given()
                            .when().get("/scaling")
                            .then()
                            .statusCode(OK.getStatusCode())
                            .extract().asString();
            uniqueIds.add(uniqueId);

            assertThat(uniqueIds).hasSize(2);
        });
    }

    /**
     * Workflow:
     * * Scale to two replicas and verify that both of the replicas are responding.
     * * Scale down to a single replica.
     * * Execute an arbitrary minimal sample of requests and verify that all are served by the same replica.
     */
    @Test
    public void scaleDownTest() {
        openShiftUtil.scale(DC_NAME, 2);

        assertThat(openShiftUtil.countReadyReplicas(DC_NAME)).isEqualTo(2);

        Set<String> uniqueIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            String uniqueId =
                    given()
                            .when().get("/scaling")
                            .then()
                            .statusCode(OK.getStatusCode())
                            .extract().asString();
            uniqueIds.add(uniqueId);

            assertThat(uniqueIds).hasSize(2);
        });

        openShiftUtil.scale(DC_NAME, 1);

        assertThat(openShiftUtil.countReadyReplicas(DC_NAME)).isEqualTo(1);

        uniqueIds.clear();
        for (int i = 0; i < 100; i++) {
            String uniqueId =
                    given()
                            .when().get("/scaling")
                            .then()
                            .statusCode(OK.getStatusCode())
                            .extract().asString();
            uniqueIds.add(uniqueId);
        }

        assertThat(uniqueIds).hasSize(1);

        assertThat(openShiftUtil.countReadyReplicas(DC_NAME)).isEqualTo(1);
    }

    /**
     * Workflow:
     * * Scale down to zero replicas.
     * * Execute an arbitrary minimal sample of requests and verify that all get HTTP 503 response.
     */
    @Test
    public void scaleToZero() {
        openShiftUtil.scale(DC_NAME, 0);

        assertThat(openShiftUtil.countReadyReplicas(DC_NAME)).isEqualTo(0);

        for (int i = 0; i < 100; i++) {
            given()
                    .when().get("/scaling")
                    .then()
                    .statusCode(SERVICE_UNAVAILABLE.getStatusCode());
        }
    }

}
