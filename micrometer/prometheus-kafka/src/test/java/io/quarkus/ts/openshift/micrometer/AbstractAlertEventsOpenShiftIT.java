package io.quarkus.ts.openshift.micrometer;

import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.SseEventSource;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractAlertEventsOpenShiftIT {

    private static final String PATH = "/monitor/stream";
    private static final String KAFKA_CONSUMER_COUNT_METRIC = "kafka_consumer_response_total";
    private static final String KAFKA_PRODUCER_COUNT_METRIC = "kafka_producer_response_total";
    private static final int WAIT_FOR_ALERTS_COUNT = 1;

    private static final String PROMETHEUS_NAMESPACE = "openshift-user-workload-monitoring";
    private static final String PROMETHEUS_POD = "prometheus-user-workload-0";
    private static final String PROMETHEUS_CONTAINER = "prometheus";

    private static final int TIMEOUT_SEC = 25;

    @TestResource
    private URL sseEndpoint;

    @TestResource
    private OpenShiftUtil openShiftUtil;

    private List<String> receive = new CopyOnWriteArrayList<>();

    @Test
    public void testAlertMonitorEventStream() throws Exception {
        whenWaitUntilReceiveSomeAlerts();
        thenExpectedAlertsHaveBeenConsumed();
        thenKafkaProducerMetricsAreFound();
        thenKafkaConsumerMetricsAreFound();
    }

    private void whenWaitUntilReceiveSomeAlerts() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(WAIT_FOR_ALERTS_COUNT);

        WebTarget target = ClientBuilder.newClient().target(sseEndpoint.toString() + PATH);
        SseEventSource source = SseEventSource.target(target).build();
        source.register(inboundSseEvent -> {
            receive.add(inboundSseEvent.readData(String.class, MediaType.APPLICATION_JSON_TYPE));
            latch.countDown();
        });

        source.open();
        latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        source.close();
    }

    private void thenExpectedAlertsHaveBeenConsumed() {
        assertEquals(WAIT_FOR_ALERTS_COUNT, receive.size(), "Unexpected number of alerts consumed");
    }

    private void thenKafkaProducerMetricsAreFound() throws Exception {
        thenMetricIsExposedInServiceEndpoint(KAFKA_PRODUCER_COUNT_METRIC, greater(0));
        thenMetricIsExposedInPrometheus(KAFKA_PRODUCER_COUNT_METRIC, any());
    }

    private void thenKafkaConsumerMetricsAreFound() throws Exception {
        thenMetricIsExposedInServiceEndpoint(KAFKA_CONSUMER_COUNT_METRIC, greater(0));
        thenMetricIsExposedInPrometheus(KAFKA_CONSUMER_COUNT_METRIC, any());
    }

    private void thenMetricIsExposedInPrometheus(String name, Predicate<String> valueMatcher) throws Exception {
        await().ignoreExceptions().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            String output = openShiftUtil.execOnPod(PROMETHEUS_NAMESPACE, PROMETHEUS_POD, PROMETHEUS_CONTAINER, "curl",
                    "http://localhost:9090/api/v1/query?query=" + name);

            assertTrue(output.contains("\"status\":\"success\""), "Verify the status was ok");
            assertTrue(output.contains("\"__name__\":\"" + name + "\""), "Verify the metrics is found");
            assertTrue(valueMatcher.test(output), "Verify the metrics contains the correct number");
        });
    }

    private static void thenMetricIsExposedInServiceEndpoint(String name, Predicate<Double> valueMatcher) {
        await().ignoreExceptions().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            String response = get("/q/metrics").then()
                    .statusCode(200)
                    .extract().asString();

            boolean matches = false;
            for (String line : response.split("[\r\n]+")) {
                if (line.startsWith(name)) {
                    Double value = extractValueFromMetric(line);
                    assertTrue(valueMatcher.test(value), "Metric value is not expected. Found: " + value);
                    matches = true;
                    break;
                }
            }

            assertTrue(matches, "Metric " + name + " not found in " + response);
        });
    }

    private static final <T> Predicate<T> any() {
        return actual -> true;
    }

    private static final Predicate<Double> greater(double expected) {
        return actual -> actual > expected;
    }

    private static Double extractValueFromMetric(String line) {
        return Double.parseDouble(line.substring(line.lastIndexOf(" ")));
    }
}
