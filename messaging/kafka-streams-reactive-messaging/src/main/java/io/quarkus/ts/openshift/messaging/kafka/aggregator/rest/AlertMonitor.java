package io.quarkus.ts.openshift.messaging.kafka.aggregator.rest;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

@Path("/")
public class AlertMonitor {

    @Inject
    @Channel("login-alerts")
    Publisher<String> alerts;

    @GET
    public Response ok() {
        return Response.ok().build();
    }

    @GET
    @Path("/monitor/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType("application/json")
    public Publisher<String> stream() {
        return alerts;
    }

}
