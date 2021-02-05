package io.quarkus.ts.openshift.micrometer;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
