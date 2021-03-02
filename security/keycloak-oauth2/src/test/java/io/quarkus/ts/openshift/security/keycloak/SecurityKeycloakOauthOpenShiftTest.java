package io.quarkus.ts.openshift.security.keycloak;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.ts.openshift.common.resources.KeycloakQuarkusTestResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@QuarkusTest
@QuarkusTestResource(KeycloakQuarkusTestResource.WithOAuth2Config.class)
public class SecurityKeycloakOauthOpenShiftTest extends AbstractSecurityKeycloakOpenShiftTest {

    @ConfigProperty(name = "quarkus.oauth2.introspection-url")
    String oauth2IntrospectionUrl;

    @ConfigProperty(name = "quarkus.http.test-port")
    Integer appPort;

    @Override
    protected String getAppUrl() {
        return "http://localhost:" + appPort;
    }

    @Override
    protected String getOAuth2IntrospectionUrl() {
        return oauth2IntrospectionUrl;
    }
}
