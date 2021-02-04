package io.quarkus.ts.openshift.micrometer;

import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;

import java.time.Duration;

@ApplicationScoped
public class AlertProducer {

    @Outgoing(Channels.CHANNEL_SOURCE_ALERTS)
    public Multi<String> generate() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(5)).map(tick -> "alert" + tick);
    }

}
