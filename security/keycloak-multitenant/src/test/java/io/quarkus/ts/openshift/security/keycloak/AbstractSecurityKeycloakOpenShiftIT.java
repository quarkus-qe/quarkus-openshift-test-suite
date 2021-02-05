package io.quarkus.ts.openshift.security.keycloak;

import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.injection.WithName;

import java.net.URL;

public abstract class AbstractSecurityKeycloakOpenShiftIT extends AbstractSecurityKeycloakOpenShiftTest {

    @TestResource
    private URL applicationUrl;

    @TestResource @WithName("keycloak-plain")
    private URL keycloakUrl;

    @Override
    protected String getAuthServerUrl() {
        return keycloakUrl.toString() + "/auth/realms/test-realm";
    }

    @Override
    protected String getAppUrl() {
        return applicationUrl.toString();
    }
}
