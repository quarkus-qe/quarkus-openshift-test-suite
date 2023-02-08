package io.quarkus.ts.openshift.infinispan.client;

import jakarta.ws.rs.Path;

@Path("/first-counter")
public class FirstCounterResource extends InfinispanCounterResource {
}
