package io.quarkus.ts.openshift.infinispan.client;

import io.quarkus.infinispan.client.Remote;
import org.infinispan.client.hotrod.RemoteCache;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.concurrent.atomic.AtomicInteger;

public class InfinispanCounterResource {
    protected AtomicInteger counter = new AtomicInteger(0);

    @Inject
    @Remote("mycache")
    RemoteCache<String, Integer> cache;

    @Path("/get-cache")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Integer getCacheCounter() {
        return cache.get("counter");
    }

    @Path("/get-client")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int getClientCounter() {
        return counter.get();
    }

    @Path("/increment-counters")
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public String incCounters() {
        int invocationNumber = counter.incrementAndGet();
        cache.put("counter", cache.get("counter") + 1);
        return "Cache=" + cache.get("counter") + " Client=" + invocationNumber;
    }

    @Path("/reset-cache")
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public String resetCacheCounter() {
        cache.put("counter", 0);
        return "Cache=" + cache.get("counter");
    }

    @Path("/reset-client")
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public String resetClientCounter() {
        counter.set(0);
        return "Client=" + counter.get();
    }
}
