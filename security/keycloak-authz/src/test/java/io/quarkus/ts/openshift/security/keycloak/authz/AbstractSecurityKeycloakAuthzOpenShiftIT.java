package io.quarkus.ts.openshift.security.keycloak.authz;

import java.net.URL;

import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.injection.WithName;

public abstract class AbstractSecurityKeycloakAuthzOpenShiftIT extends AbstractSecurityKeycloakAuthzOpenShiftTest {

    @TestResource
    @WithName("keycloak-plain")
    private URL keycloakUrl;

    @Override
    protected String getAuthServerUrl() {
        return keycloakUrl.toString() + "/auth/realms/test-realm";
    }
}
