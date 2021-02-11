package io.quarkus.ts.openshift.http.clients;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@RegisterRestClient
public interface HealthClientService {

    @GET
    @Path("/health")
    Response health();
}
