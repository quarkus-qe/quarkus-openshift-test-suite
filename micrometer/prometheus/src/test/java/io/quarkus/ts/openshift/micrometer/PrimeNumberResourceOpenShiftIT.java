package io.quarkus.ts.openshift.micrometer;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;

/**
 * The application contains a `PrimeNumberResource` resource that generates a few metrics:
 * - `prime_number_max_{uniqueId}`: max prime number that is found
 * - `prime_number_test_{uniqueId}`: with information about the calculation of the prime number
 */
@OpenShiftTest
@AdditionalResources("classpath:service-monitor.yaml")
public class PrimeNumberResourceOpenShiftIT {

    private static final String PRIME_NUMBER_MAX = "prime_number_max_%s";

    private static final String PRIME_NUMBER_TEST_COUNT = "prime_number_test_%s_seconds_count";
    private static final String PRIME_NUMBER_TEST_MAX = "prime_number_test_%s_seconds_max";
    private static final String PRIME_NUMBER_TEST_SUM = "prime_number_test_%s_seconds_sum";

    private static final String PROMETHEUS_NAMESPACE = "openshift-user-workload-monitoring";
    private static final String PROMETHEUS_POD = "prometheus-user-workload-0";
    private static final String PROMETHEUS_CONTAINER = "prometheus";

    private static final Integer ANY_VALUE = null;

    @TestResource
    private OpenShiftUtil openShiftUtil;

    private String uniqueId;

    @BeforeEach
    public void setup() {
        uniqueId = get("/uniqueId").then().statusCode(HttpStatus.SC_OK).extract().asString();
    }

    @Test
    public void primeNumberCustomMetricsShouldBeExposed() throws Exception {
        whenCheckPrimeNumber(3); // It's prime, so it should set the prime.number.max metric.
        whenCheckPrimeNumber(4); // It's not prime, so It's ignored.

        thenMetricIsExposedInServiceEndpoint(PRIME_NUMBER_MAX, 3);
        thenMetricIsExposedInServiceEndpoint(PRIME_NUMBER_TEST_COUNT, 1);
        thenMetricIsExposedInServiceEndpoint(PRIME_NUMBER_TEST_MAX, ANY_VALUE);
        thenMetricIsExposedInServiceEndpoint(PRIME_NUMBER_TEST_SUM, ANY_VALUE);

        thenMetricIsExposedInPrometheus(PRIME_NUMBER_MAX, 3);
        thenMetricIsExposedInPrometheus(PRIME_NUMBER_TEST_COUNT, 1);
        thenMetricIsExposedInPrometheus(PRIME_NUMBER_TEST_MAX, ANY_VALUE);
        thenMetricIsExposedInPrometheus(PRIME_NUMBER_TEST_SUM, ANY_VALUE);
    }

    private void whenCheckPrimeNumber(int number) {
        get("/check/" + number).then().statusCode(HttpStatus.SC_OK);
    }

    private void thenMetricIsExposedInPrometheus(String name, Integer expected) throws Exception {
        await().ignoreExceptions().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            String output = openShiftUtil.execOnPod(PROMETHEUS_NAMESPACE, PROMETHEUS_POD, PROMETHEUS_CONTAINER, "curl",
                    "http://localhost:9090/api/v1/query?query=" + primeNumberCustomMetricName(name));

            assertTrue(output.contains("\"status\":\"success\""), "Verify the status was ok");
            assertTrue(output.contains("\"__name__\":\"" + primeNumberCustomMetricName(name) + "\""),
                    "Verify the metrics is found");
            if (expected != null) {
                assertTrue(output.contains("\"" + expected + "\""), "Verify the metrics contains the correct number");
            }

        });
    }

    private void thenMetricIsExposedInServiceEndpoint(String name, Integer expected) {
        await().ignoreExceptions().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            String shouldContain = primeNumberCustomMetricName(name);
            if (expected != null) {
                shouldContain += " " + expected;
            }

            get("/q/metrics").then()
                    .statusCode(200)
                    .body(containsString(shouldContain));
        });
    }

    private String primeNumberCustomMetricName(String metricName) {
        return String.format(metricName, uniqueId);
    }
}
