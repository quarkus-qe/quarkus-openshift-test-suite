package io.quarkus.ts.openshift.micrometer;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class AlertConsumer {

    @Inject
    @Channel(Channels.ALERTS_STREAM)
    Publisher<String> alerts;

    @GET
    @Path("/monitor/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.TEXT_PLAIN)
    public Publisher<String> stream() {
        return alerts;
    }

}
