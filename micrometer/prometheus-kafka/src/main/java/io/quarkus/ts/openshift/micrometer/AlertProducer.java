package io.quarkus.ts.openshift.micrometer;

import java.time.Duration;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AlertProducer {

    @Outgoing(Channels.CHANNEL_SOURCE_ALERTS)
    public Multi<String> generate() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(5)).map(tick -> "alert" + tick);
    }

}
