package io.quarkus.ts.openshift.security.keycloak.authz;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.ts.openshift.common.resources.KeycloakQuarkusTestResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@QuarkusTest
@QuarkusTestResource(KeycloakQuarkusTestResource.WithOidcConfig.class)
public class SecurityKeycloakAuthzOpenShiftTest extends AbstractSecurityKeycloakAuthzOpenShiftTest {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String oidcAuthServerUrl;

    @Override
    protected String getAuthServerUrl() {
        return oidcAuthServerUrl;
    }
}
