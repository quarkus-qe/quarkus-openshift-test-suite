package io.quarkus.ts.openshift.microprofile;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

@RegisterRestClient(baseUri = "http://microprofile-test:8080/")
public interface HelloClient {
    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    @Asynchronous
    @Fallback(fallbackMethod = "fallback")
    CompletionStage<String> get();

    default CompletionStage<String> fallback() {
        return completedFuture("Fallback");
    }
}
