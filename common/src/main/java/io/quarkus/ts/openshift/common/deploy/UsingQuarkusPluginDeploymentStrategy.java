package io.quarkus.ts.openshift.common.deploy;

import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.common.Command;
import io.quarkus.ts.openshift.common.injection.TestResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * If the test class is using the {@code UsingQuarkusPluginDeploymentStrategy} strategy, the test framework will use the
 * Quarkus OpenShift extension to deploy the Quarkus Application by using the property `quarkus.kubernetes.deploy`.
 *
 */
public class UsingQuarkusPluginDeploymentStrategy implements DeploymentStrategy {

    private static final String MVN_COMMAND = "mvn";
    private static final String PACKAGE_GOAL = "package";
    private static final String QUARKUS_PLUGIN_DEPLOY = "-Dquarkus.kubernetes.deploy=true";
    private static final String MVN_REPOSITORY_LOCAL = "maven.repo.local";
    private static final String SKIP_TESTS = "-DskipTests=true";
    private static final String SKIP_ITS = "-DskipITs=true";
    private static final String BATCH_MODE = "-B";
    private static final String DISPLAY_VERSION = "-V";
    private static final String QUARKUS_KUBERNETES_CLIENT_NAMESPACE = "quarkus.kubernetes-client.namespace";
    private static final String QUARKUS_KUBERNETES_CLIENT_TRUST_CERTS = "quarkus.kubernetes-client.trust-certs";
    private static final String QUARKUS_CONTAINER_IMAGE_GROUP = "quarkus.container-image.group";
    private static final String QUARKUS_NATIVE_BUILDER_IMAGE = "quarkus.native.builder-image";
    private static final String QUARKUS_NATIVE_CONTAINER_RUNTIME = "quarkus.native.container-runtime";
    private static final String QUARKUS_NATIVE_MEMORY_LIMIT = "quarkus.native.native-image-xmx";
    private static final String QUARKUS_PACKAGE_TYPE = "quarkus.package.type";
    private static final String QUARKUS_OPENSHIFT_ENV_VARS = "quarkus.openshift.env.vars.";
    private static final String OC_IGNORE_IF_NOT_FOUND = "--ignore-not-found=true";

    private static final String NATIVE = "native";
    private static final String DOCKER = "docker";
    private static final String DEFAULT_NATIVE_MEMORY_LIMIT = "3g";

    @TestResource
    private OpenShiftClient openShiftClient;

    @Override
    public void deploy(Map<String, String> envVars) throws Exception {
        String namespace = openShiftClient.getNamespace();

        List<String> args = new ArrayList<>(
                Arrays.asList(MVN_COMMAND, BATCH_MODE, DISPLAY_VERSION, PACKAGE_GOAL, QUARKUS_PLUGIN_DEPLOY, SKIP_TESTS,
                        SKIP_ITS));
        args.add(withKubernetesClientNamespace(namespace));
        args.add(withKubernetesClientTrustCerts());
        args.add(withContainerImageGroup(namespace));
        withMavenRepositoryLocalIfSet(args);
        withNativeBuildArgumentsIfNative(args);
        withEnvVars(args, envVars);

        new Command(args).runAndWait();
    }

    @Override
    public void undeploy() throws Exception {
        for (Path resourceToDelete : findKubernetesResources()) {
            new Command("oc", "delete", "-f", resourceToDelete.toFile().getAbsolutePath(), OC_IGNORE_IF_NOT_FOUND).runAndWait();
        }
    }

    private List<Path> findKubernetesResources() throws IOException {
        try (Stream<Path> resources = Files
                .find(Paths.get("target/kubernetes/"), Integer.MAX_VALUE,
                        (path, ignored) -> path.toFile().getName().matches(".*.yml"))) {

            return resources.collect(Collectors.toList());
        }
    }

    private static final void withNativeBuildArgumentsIfNative(List<String> args) {
        if (NATIVE.equals(System.getProperty(QUARKUS_PACKAGE_TYPE))) {
            args.add(withProperty(QUARKUS_PACKAGE_TYPE, NATIVE));
            args.add(withProperty(QUARKUS_NATIVE_CONTAINER_RUNTIME,
                    System.getProperty(QUARKUS_NATIVE_CONTAINER_RUNTIME, DOCKER)));
            args.add(withProperty(QUARKUS_NATIVE_MEMORY_LIMIT,
                    System.getProperty(QUARKUS_NATIVE_MEMORY_LIMIT, DEFAULT_NATIVE_MEMORY_LIMIT)));

            String nativeBuilderImage = System.getProperty(QUARKUS_NATIVE_BUILDER_IMAGE);
            if (nativeBuilderImage != null) {
                args.add(withProperty(QUARKUS_NATIVE_BUILDER_IMAGE, nativeBuilderImage));
            }
        }
    }

    private void withMavenRepositoryLocalIfSet(List<String> args) {
        String mvnRepositoryPath = System.getProperty(MVN_REPOSITORY_LOCAL);
        if (mvnRepositoryPath != null) {
            args.add(withProperty(MVN_REPOSITORY_LOCAL, mvnRepositoryPath));
        }
    }

    private static final String withContainerImageGroup(String namespace) {
        return withProperty(QUARKUS_CONTAINER_IMAGE_GROUP, namespace);
    }

    private static final String withKubernetesClientNamespace(String namespace) {
        return withProperty(QUARKUS_KUBERNETES_CLIENT_NAMESPACE, namespace);
    }

    private static final String withKubernetesClientTrustCerts() {
        return withProperty(QUARKUS_KUBERNETES_CLIENT_TRUST_CERTS, Boolean.TRUE.toString());
    }

    private void withEnvVars(List<String> args, Map<String, String> envVars) {
        for (Entry<String, String> envVar : envVars.entrySet()) {
            args.add(withProperty(QUARKUS_OPENSHIFT_ENV_VARS + envVar.getKey(), envVar.getValue()));
        }
    }

    private static final String withProperty(String property, String value) {
        return String.format("-D%s=%s", property, value);
    }

}
