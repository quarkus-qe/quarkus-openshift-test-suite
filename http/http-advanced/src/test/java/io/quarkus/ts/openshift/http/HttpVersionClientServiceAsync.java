package io.quarkus.ts.openshift.http;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
@RegisterRestClient
public interface HttpVersionClientServiceAsync {

    @GET
    @Path("/httpVersion")
    Uni<Response> getClientHttpVersion();
}
