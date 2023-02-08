package io.quarkus.ts.openshift.security.keycloak;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Authenticated
@Path("/user")
@Produces(MediaType.TEXT_PLAIN)
public class UserResource {

    private static final String TENANT_ID_ATTR = "tenant-id";

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/webapp-tenant")
    public String getUsingWebAppTenant() {
        return producesMessage();
    }

    @GET
    @Path("/jwt-tenant")
    public String getUsingJwtTenant() {
        return producesMessage();
    }

    @GET
    @Path("/service-tenant")
    public String getUsingServiceTenant() {
        return producesMessage();
    }

    private String producesMessage() {
        return "Hello, user " + identity.getPrincipal().getName() + " using tenant "
                + identity.getAttributes().get(TENANT_ID_ATTR);
    }
}
