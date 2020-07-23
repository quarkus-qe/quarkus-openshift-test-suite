package io.quarkus.ts.openshift.security.https.twoway.authz;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

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
