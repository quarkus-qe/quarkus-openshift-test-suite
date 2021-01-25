package io.quarkus.ts.openshift.security.keycloak;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OnlyIfNotConfigured;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@AdditionalResources("classpath:deployments/keycloak/version-73.yaml")
@AdditionalResources("classpath:keycloak-realm.yaml")
@AdditionalResources("classpath:deployments/keycloak/deployment.yaml")
@OnlyIfNotConfigured("ts.authenticated-registry")
public class SecurityKeycloak73OpenShiftIT extends AbstractSecurityKeycloakOpenShiftIT {
}
