package io.quarkus.ts.openshift.security.keycloak;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.CustomizeApplicationDeployment;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.injection.WithName;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public abstract class AbstractSecurityKeycloakOpenShiftIT extends AbstractSecurityKeycloakOpenShiftTest {

    static String oauth2IntrospectionUrl;

    // TODO this is pretty ugly, but I'm tired and can't think of a better way at the moment
    @CustomizeApplicationDeployment
    public static void configureKeycloakUrl(OpenShiftClient oc, AppMetadata appMetadata, @WithName("keycloak-plain") URL url)
            throws IOException {
        oauth2IntrospectionUrl = url + "/auth/realms/test-realm/protocol/openid-connect/token/introspect";

        List<HasMetadata> objs = oc.load(Files.newInputStream(Paths.get("target/kubernetes/openshift.yml"))).get();
        objs.stream()
                .filter(it -> it instanceof DeploymentConfig)
                .filter(it -> it.getMetadata().getName().equals(appMetadata.appName))
                .map(DeploymentConfig.class::cast)
                .forEach(dc -> {
                    dc.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                        container.getEnv().add(
                                new EnvVar("QUARKUS_OAUTH2_INTROSPECTION_URL", oauth2IntrospectionUrl, null));
                    });
                });

        KubernetesList list = new KubernetesList();
        list.setItems(objs);
        Serialization.yamlMapper().writeValue(Files.newOutputStream(Paths.get("target/kubernetes/openshift.yml")), list);
    }

    @TestResource
    private URL applicationUrl;

    @Override
    protected String getAppUrl() {
        return applicationUrl.toString();
    }

    @Override
    protected String getOAuth2IntrospectionUrl() {
        return oauth2IntrospectionUrl;
    }
}
