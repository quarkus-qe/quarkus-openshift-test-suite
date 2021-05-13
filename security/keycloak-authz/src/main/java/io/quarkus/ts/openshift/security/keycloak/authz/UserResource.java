package io.quarkus.ts.openshift.security.keycloak.authz;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/user")
public class UserResource {
    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken jwt;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Authenticated
    public String get() {
        return "Hello, user " + identity.getPrincipal().getName();
    }

    @GET
    @Path("/issuer")
    @Produces(MediaType.TEXT_PLAIN)
    @Authenticated
    public String issuer() {
        return "user token issued by " + jwt.getIssuer();
    }
}
