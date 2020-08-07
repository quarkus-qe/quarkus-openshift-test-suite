package io.quarkus.ts.openshift.scaling;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/scaling")
public class ScalingResource {

    private static UUID uuid;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        // this is not exactly thread safe and should be fixed in the future
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return uuid.toString();
    }
}
