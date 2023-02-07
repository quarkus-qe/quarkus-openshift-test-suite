package io.quarkus.ts.openshift.http.clients;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@RegisterRestClient
public interface HealthClientService {

    @GET
    @Path("/health")
    Response health();
}
