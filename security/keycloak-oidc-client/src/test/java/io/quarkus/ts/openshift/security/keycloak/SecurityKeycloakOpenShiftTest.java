package io.quarkus.ts.openshift.security.keycloak;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.ts.openshift.common.resources.KeycloakQuarkusTestResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@QuarkusTest
@QuarkusTestResource(SecurityKeycloakOpenShiftTest.CustomKeycloakQuarkusTestResource.class)
public class SecurityKeycloakOpenShiftTest extends AbstractSecurityKeycloakOpenShiftTest {

    private static final List<String> PROPERTIES_TO_SET_AUTH_URL = Arrays.asList("quarkus.oidc-client.auth-server-url",
            "quarkus.oidc-client.normal-user.auth-server-url",
            "quarkus.oidc-client.admin-user.auth-server-url",
            "quarkus.oidc-client.jwt-secret.auth-server-url");

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String oidcAuthServerUrl;

    @Override
    protected String getAuthServerUrl() {
        return oidcAuthServerUrl;
    }

    public static class CustomKeycloakQuarkusTestResource extends KeycloakQuarkusTestResource.WithOidcConfig {
        @Override
        protected Map<String, String> providesQuarkusConfiguration() {
            Map<String, String> properties = new HashMap<>(super.providesQuarkusConfiguration());
            PROPERTIES_TO_SET_AUTH_URL.forEach(property -> properties.put(property, realmAuthUrl()));
            return properties;
        }
    }
}
