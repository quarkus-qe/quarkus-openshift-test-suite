package io.quarkus.ts.openshift.microprofile;

import io.opentracing.Tracer;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.concurrent.CompletionStage;

@Path("/client")
public class ClientResource {
    @Inject
    @RestClient
    HelloClient hello;

    @Inject
    Tracer tracer;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Asynchronous
    @Retry
    public CompletionStage<String> get() {
        tracer.activeSpan().log("ClientResource called");
        return hello.get().thenApply(result -> "Client got: " + result);
    }
}
