package io.quarkus.ts.openshift.messaging.kafka;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;
import org.junit.jupiter.api.Test;

import java.net.URL;

@OpenShiftTest
@AdditionalResources("classpath:k8s-kafka.yaml")
public class KafkaStreamOpenShiftIT extends AbstractKafkaTest{

    private static final int eventsAmount = 3;

    @TestResource
    private URL sseEndpoint;

    @Test
    void testAlertMonitorEventStream() throws InterruptedException {
        givenAnApplicationEndpoint(sseEndpoint + "/monitor/stream");
        whenRequestSomeEvents(eventsAmount);
        thenVerifyAllEventsArrived();
    }
}
