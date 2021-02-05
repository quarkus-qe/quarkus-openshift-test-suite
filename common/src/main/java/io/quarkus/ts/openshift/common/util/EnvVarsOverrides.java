package io.quarkus.ts.openshift.common.util;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class EnvVarsOverrides {
    public static void apply(Map<String, String> envVars, Path yaml, OpenShiftClient oc) throws IOException {
        List<HasMetadata> objs = oc.load(Files.newInputStream(yaml)).get();

        for (HasMetadata obj : objs) {
            if (obj instanceof DeploymentConfig) {
                DeploymentConfig dc = (DeploymentConfig) obj;

                dc.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                    envVars.entrySet()
                            .forEach(envVar -> container.getEnv().add(new EnvVar(envVar.getKey(), envVar.getValue(), null)));
                });
            }
        }

        KubernetesList list = new KubernetesList();
        list.setItems(objs);
        Serialization.yamlMapper().writeValue(Files.newOutputStream(yaml), list);
    }
}
