package io.quarkus.ts.openshift.security.jwt.cookie;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@RolesAllowed("admin")
@Path("/permitted")
public class PermittedResource {
    @GET
    @PermitAll
    public String permitted() {
        return "Hello there!";
    }
}
