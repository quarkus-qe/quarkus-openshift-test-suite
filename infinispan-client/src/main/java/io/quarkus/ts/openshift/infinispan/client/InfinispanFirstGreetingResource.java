package io.quarkus.ts.openshift.infinispan.client;

import io.quarkus.infinispan.client.Remote;
import org.infinispan.client.hotrod.RemoteCache;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/first-endpoint")
public class InfinispanFirstGreetingResource {

    @Inject
    @Remote("mycache")
    RemoteCache<String, String> cache;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return cache.get("hello");
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String updateHello(String text) {
        cache.put("hello","Hello World, Infinispan is up and changed!");
        return "Updating my cache";
    }
}
