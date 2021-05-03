package io.quarkus.ts.openshift.micrometer;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class AlertProducer {

    @Outgoing(Channels.CHANNEL_SOURCE_ALERTS)
    public Multi<String> generate() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(5)).map(tick -> "alert" + tick);
    }

}
