package io.quarkus.ts.openshift.http;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.CustomizeApplicationDeployment;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.WithName;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@OpenShiftTest
@AdditionalResources("classpath:deployments/keycloak/version-73.yaml")
@AdditionalResources("classpath:keycloak-realm.yaml")
@AdditionalResources("classpath:deployments/keycloak/deployment.yaml")
public class HttpOpenShiftIT extends AbstractHttpTest{
    static String keycloakUrl;

    @CustomizeApplicationDeployment
    public static void configureKeycloakUrl(OpenShiftClient oc, AppMetadata appMetadata, @WithName("keycloak-plain") URL url) throws IOException {
        keycloakUrl = url + "/auth/realms/test-realm";

        List<HasMetadata> objs = oc.load(Files.newInputStream(Paths.get("target/kubernetes/openshift.yml"))).get();
        objs.stream()
                .filter(it -> it instanceof DeploymentConfig)
                .filter(it -> it.getMetadata().getName().equals(appMetadata.appName))
                .map(DeploymentConfig.class::cast)
                .forEach(dc -> {
                    dc.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                        container.getEnv().add(
                                new EnvVar("QUARKUS_OIDC_AUTH_SERVER_URL", keycloakUrl, null)
                        );
                    });
                });

        KubernetesList list = new KubernetesList();
        list.setItems(objs);
        Serialization.yamlMapper().writeValue(Files.newOutputStream(Paths.get("target/kubernetes/openshift.yml")), list);
    }

    @Override
    protected String getAuthServerUrl() {
        return keycloakUrl;
    }
}
