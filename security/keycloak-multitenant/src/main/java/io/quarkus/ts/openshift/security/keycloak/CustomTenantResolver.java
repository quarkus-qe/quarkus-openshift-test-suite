package io.quarkus.ts.openshift.security.keycloak;

import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.ApplicationScoped;

import java.util.stream.Stream;

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
