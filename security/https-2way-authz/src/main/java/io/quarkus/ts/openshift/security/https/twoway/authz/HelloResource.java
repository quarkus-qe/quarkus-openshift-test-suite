package io.quarkus.ts.openshift.security.https.twoway.authz;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/")
public class HelloResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@Context SecurityContext security) {
        String user = security.getUserPrincipal().getName();
        if ("".equals(user)) {
            user = "<anonymous>";
        }

        return "Hello " + user
                + ", HTTPS: " + security.isSecure()
                + ", isUser: " + security.isUserInRole("user")
                + ", isGuest: " + security.isUserInRole("guest");
    }
}
