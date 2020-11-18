package io.quarkus.ts.openshift.messaging.kafka;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(StrimziKafkaResource.class)
public class KafkaStreamsTest extends AbstractKafkaTest{

    private static final String DEFAULT_SSE_ENDPOINT = "http://localhost:8080/monitor/stream";
    private static final String sseEndpoint = ConfigProvider.getConfig().getOptionalValue("sse.endpoint", String.class).orElse(DEFAULT_SSE_ENDPOINT);
    private static final int eventsAmount = 3;

    @Test
    void testAlertMonitorEventStream() throws InterruptedException {
        givenAnApplicationEndpoint(sseEndpoint);
        whenRequestSomeEvents(eventsAmount);
        thenVerifyAllEventsArrived();
    }
}
