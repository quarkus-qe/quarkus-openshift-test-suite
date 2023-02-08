package io.quarkus.ts.openshift.infinispan.client;

import jakarta.ws.rs.Path;

@Path("/second-counter")
public class SecondCounterResource extends InfinispanCounterResource {
}
