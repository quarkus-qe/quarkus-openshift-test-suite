package io.quarkus.ts.openshift.common.deploy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.Command;
import io.quarkus.ts.openshift.common.OpenShiftTestException;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.AwaitUtil;
import io.quarkus.ts.openshift.common.util.EnvVarsOverrides;
import io.quarkus.ts.openshift.common.util.ImageOverrides;
import io.quarkus.ts.openshift.common.util.NamespaceOverrides;

/**
 * If the test class is using the {@code EmbeddedDeploymentStrategy} strategy, the test framework will push the resources into
 * OpenShift and
 * create the necessary components.
 *
 */
public class EmbeddedDeploymentStrategy implements DeploymentStrategy {

    @TestResource
    private OpenShiftClient openShiftClient;

    @TestResource
    private AppMetadata appMetadata;

    @TestResource
    private AwaitUtil awaitUtil;

    @Override
    public void deploy(Map<String, String> envVars) throws Exception {
        Path openshiftResources = getResourcesYaml();
        if (!Files.exists(openshiftResources)) {
            throw new OpenShiftTestException(
                    "Missing " + openshiftResources + ", did you add the quarkus-kubernetes or quarkus-openshift extension?");
        }

        NamespaceOverrides.apply(openshiftResources, openShiftClient);
        ImageOverrides.apply(openshiftResources, openShiftClient);
        EnvVarsOverrides.apply(envVars, openshiftResources, openShiftClient);

        System.out.println("deploying application");
        new Command("oc", "apply", "-f", openshiftResources.toString()).runAndWait();

        awaitImageStreams(openshiftResources);

        Optional<String> binary = findNativeBinary();
        if (binary.isPresent()) {
            new Command("oc", "start-build", appMetadata.appName, "--from-file=" + binary.get(), "--follow")
                    .runAndWait();
        } else {
            new Command("oc", "start-build", appMetadata.appName, "--from-dir=target/quarkus-app", "--follow").runAndWait();
        }
    }

    @Override
    public void undeploy() throws Exception {
        System.out.println("undeploying application");
        new Command("oc", "delete", "-f", getResourcesYaml().toString(), "--ignore-not-found").runAndWait();
    }

    private Path getResourcesYaml() {
        return Paths.get("target", "kubernetes", "openshift.yml");
    }

    private Optional<String> findNativeBinary() throws Exception {
        try (Stream<Path> binariesFound = Files
                .find(Paths.get("target/"), Integer.MAX_VALUE,
                        (path, basicFileAttributes) -> path.toFile().getName().matches(".*-runner"))) {
            return binariesFound.map(path -> path.normalize().toString()).findFirst();
        }
    }

    private void awaitImageStreams(Path openshiftResources) throws IOException {
        openShiftClient.load(Files.newInputStream(openshiftResources))
                .get()
                .stream()
                .flatMap(it -> it instanceof ImageStream ? Stream.of(it) : Stream.empty())
                .map(it -> it.getMetadata().getName())
                .filter(it -> !it.equals(appMetadata.appName))
                .forEach(awaitUtil::awaitImageStream);
    }

}
