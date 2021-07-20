package io.quarkus.ts.openshift.app.metadata;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;

public class AppMetadataCollector {
    @BuildStep
    public void collectAppMetadata(ContainerImageInfoBuildItem containerImage,
            HttpRootPathBuildItem httpRoot,
            Optional<KubernetesHealthLivenessPathBuildItem> liveness,
            Optional<KubernetesHealthReadinessPathBuildItem> readiness,
            BuildProducer<GeneratedFileSystemResourceBuildItem> output) {

        String image = containerImage.getImage();
        int lastSlash = image.lastIndexOf('/');
        int lastColon = image.lastIndexOf(':');
        String appName = image.substring(lastSlash + 1, lastColon);

        // paths to Kubernetes probes are already httpRoot-adjusted
        String knownEndpoint;
        if (readiness.isPresent()) {
            knownEndpoint = readiness.get().getPath();
        } else if (liveness.isPresent()) {
            knownEndpoint = liveness.get().getPath();
        } else {
            knownEndpoint = httpRoot.resolvePath("/"); // TODO ?
        }

        String deploymentTarget = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.kubernetes.deployment-target", String.class).orElse("");

        AppMetadata result = new AppMetadata(
                appName,
                httpRoot.getRootPath(),
                knownEndpoint,
                deploymentTarget);

        output.produce(new GeneratedFileSystemResourceBuildItem(
                "app-metadata.properties",
                result.toString().getBytes(StandardCharsets.UTF_8)));
    }
}
