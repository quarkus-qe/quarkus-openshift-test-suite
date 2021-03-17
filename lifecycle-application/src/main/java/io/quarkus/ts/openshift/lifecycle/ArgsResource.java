package io.quarkus.ts.openshift.lifecycle;

import io.quarkus.runtime.annotations.CommandLineArguments;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.stream.Collectors;
import java.util.stream.Stream;

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
