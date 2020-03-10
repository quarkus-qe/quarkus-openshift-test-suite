package io.quarkus.ts.openshift.app.metadata;

import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class AppMetadataCollector {
    @BuildStep
    public void collectAppMetadata(ContainerImageInfoBuildItem containerImage,
                                   HttpRootPathBuildItem httpRoot,
                                   Optional<KubernetesHealthLivenessPathBuildItem> liveness,
                                   Optional<KubernetesHealthReadinessPathBuildItem> readiness,
                                   BuildProducer<GeneratedFileSystemResourceBuildItem> output) {

        // paths to Kubernetes probes are already httpRoot-adjusted
        String knownEndpoint;
        if (readiness.isPresent()) {
            knownEndpoint = readiness.get().getPath();
        } else if (liveness.isPresent()) {
            knownEndpoint = liveness.get().getPath();
        } else {
            knownEndpoint = httpRoot.adjustPath("/"); // TODO ?
        }

        AppMetadata result = new AppMetadata(
                containerImage.getName(),
                httpRoot.getRootPath(),
                knownEndpoint
        );

        output.produce(new GeneratedFileSystemResourceBuildItem(
                "app-metadata.properties",
                result.toString().getBytes(StandardCharsets.UTF_8)
        ));
    }
}
