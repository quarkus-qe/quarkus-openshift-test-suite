package io.quarkus.ts.openshift.http;

import io.quarkus.example.GreeterGrpc;
import io.quarkus.example.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/grpc")
public class GrpcResource {

    @Inject
    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub client;

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("name") String name) {
        return client.sayHello(HelloRequest.newBuilder().setName(name).build()).getMessage();
    }

}
