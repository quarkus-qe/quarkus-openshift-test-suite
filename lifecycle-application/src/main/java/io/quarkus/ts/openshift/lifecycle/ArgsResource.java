package io.quarkus.ts.openshift.lifecycle;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.runtime.annotations.CommandLineArguments;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/args")
public class ArgsResource {
    @Inject
    @CommandLineArguments
    String[] args;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return Stream.of(args).collect(Collectors.joining(","));
    }
}
