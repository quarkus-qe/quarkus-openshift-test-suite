package io.quarkus.ts.openshift.config.secret.file.system;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
