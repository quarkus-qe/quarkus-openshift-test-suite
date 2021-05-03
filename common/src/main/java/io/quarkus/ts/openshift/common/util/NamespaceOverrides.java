package io.quarkus.ts.openshift.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.client.OpenShiftClient;

public final class NamespaceOverrides {
    public static void apply(Path yaml, OpenShiftClient oc) throws IOException {
        String namespace = oc.getNamespace();
        List<HasMetadata> objs = oc.load(Files.newInputStream(yaml)).get();

        for (HasMetadata obj : objs) {
            obj.getMetadata().setNamespace(namespace);
        }

        KubernetesList list = new KubernetesList();
        list.setItems(objs);
        Serialization.yamlMapper().writeValue(Files.newOutputStream(yaml), list);
    }
}
