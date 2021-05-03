package io.quarkus.ts.openshift.security.keycloak;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.ts.openshift.common.resources.KeycloakQuarkusTestResource;

@QuarkusTest
@QuarkusTestResource(KeycloakQuarkusTestResource.WithOidcConfig.class)
public class SecurityKeycloakMultitenantOpenShiftTest extends AbstractSecurityKeycloakOpenShiftTest {

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
}
