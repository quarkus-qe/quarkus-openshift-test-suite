package io.quarkus.ts.openshift.messaging.kafka;

import io.quarkus.ts.openshift.messaging.kafka.aggregator.model.LoginAggregation;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.SseEventSource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractKafkaTest {

    private String endpoint;
    private Client client = ClientBuilder.newClient();
    private List<LoginAggregation> receive = new CopyOnWriteArrayList<>();
    private static final int TIMEOUT_SEC = 25;
    private boolean completed;

    public void givenAnApplicationEndpoint(String endpoint) {
       this.endpoint = endpoint;
    }

    public void whenRequestSomeEvents(int amount) throws InterruptedException {
        WebTarget target = client.target(endpoint);
        final CountDownLatch latch = new CountDownLatch(amount);

        SseEventSource source = SseEventSource.target(target).build();
        source.register(inboundSseEvent -> {
            receive.add(inboundSseEvent.readData(LoginAggregation.class, MediaType.APPLICATION_JSON_TYPE));
            latch.countDown();
        });

        source.open();
        completed = latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        source.close();
    }

    public void thenVerifyAllEventsArrived(){
        assertEquals(true, completed);
    }
}
