package io.quarkus.ts.openshift.microprofile;

import io.opentracing.Tracer;
import org.eclipse.microprofile.opentracing.Traced;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;

@Path("/hello")
@Produces(MediaType.TEXT_PLAIN)
public class HelloResource {
    @Inject
    HelloService hello;

    @Inject
    Tracer tracer;

    @GET
    public CompletionStage<String> get(@QueryParam("name") @DefaultValue("World") String name) {
        tracer.activeSpan().log("HelloResource called");
        return hello.get(name);
    }

    @POST
    @Traced(false)
    @Path("/enable")
    public void enable() {
        hello.enable();
    }

    @POST
    @Traced(false)
    @Path("/disable")
    public void disable() {
        hello.disable();
    }
}
