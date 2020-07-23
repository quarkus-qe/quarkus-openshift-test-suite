package io.quarkus.ts.openshift.security.https.twoway;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Path("/hello")
public class HelloResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@Context SecurityContext security) {
        return "Hello, HTTPS: " + security.isSecure();
    }
}
