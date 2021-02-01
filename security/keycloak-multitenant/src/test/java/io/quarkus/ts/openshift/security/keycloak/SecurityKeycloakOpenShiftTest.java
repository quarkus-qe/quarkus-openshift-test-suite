package io.quarkus.ts.openshift.security.keycloak;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.ts.openshift.common.resources.KeycloakQuarkusTestResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@QuarkusTest
@QuarkusTestResource(SecurityKeycloakOpenShiftTest.CustomKeycloakQuarkusTestResource.class)
public class SecurityKeycloakOpenShiftTest extends AbstractSecurityKeycloakOpenShiftTest {

    private static final String OIDC_TENANT_AUTH_URL_PROPERTY = "quarkus.oidc.%s.auth-server-url";
    private static final String OIDC_JWT_TENANT_AUTH_TOKEN_ISSUER = "quarkus.oidc.jwt-tenant.token.issuer";

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String oidcAuthServerUrl;

    @ConfigProperty(name = "quarkus.http.test-port")
    Integer appPort;

    @Override
    protected String getAppUrl() {
        return "http://localhost:" + appPort;
    }

    @Override
    protected String getAuthServerUrl() {
        return oidcAuthServerUrl;
    }

    public static class CustomKeycloakQuarkusTestResource extends KeycloakQuarkusTestResource.WithOidcConfig {
        @Override
        protected Map<String, String> providesQuarkusConfiguration() {
            Map<String, String> properties = new HashMap<>(super.providesQuarkusConfiguration());
            Stream.of(Tenant.values())
                    .forEach(t -> properties.put(String.format(OIDC_TENANT_AUTH_URL_PROPERTY, t.getValue()), realmAuthUrl()));

            properties.put(OIDC_JWT_TENANT_AUTH_TOKEN_ISSUER, realmAuthUrl());
            return properties;
        }
    }
}
