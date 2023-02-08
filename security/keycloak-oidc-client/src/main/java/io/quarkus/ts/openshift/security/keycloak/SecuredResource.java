package io.quarkus.ts.openshift.security.keycloak;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/secured")
public class SecuredResource {
    @Inject
    SecurityIdentity identity;

    @GET
    @RolesAllowed("**")
    public String get() {
        return "Hello, user " + identity.getPrincipal().getName();
    }
}
