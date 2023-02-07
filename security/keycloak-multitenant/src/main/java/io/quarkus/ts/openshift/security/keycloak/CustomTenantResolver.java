package io.quarkus.ts.openshift.security.keycloak;

import java.util.stream.Stream;

import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        String path = context.request().path();

        return Stream.of(Tenant.values())
                .map(Tenant::getValue)
                .filter(path::contains)
                .findFirst()
                .orElse(null);
    }
}
