package io.quarkus.ts.openshift.config.secret.file.system;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Optional;

@Path("/hello")
public class HelloResource {
    @ConfigProperty(name = "hello.message")
    Optional<String> message;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@QueryParam("name") @DefaultValue("World") String name) {
        if (message.isPresent()) {
            return Response.ok().entity(new Hello(String.format(message.get(), name))).build();
        }

        return Response.status(500).entity("Secret not present").build();
    }
}
