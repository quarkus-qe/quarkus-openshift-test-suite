package io.quarkus.ts.openshift.common;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.common.util.AwaitUtil;
import io.quarkus.ts.openshift.common.util.ImageOverrides;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

final class AdditionalResourcesDeployed implements CloseableResource {

    private final String url;
    private final Path file;
    private final TestsStatus testsStatus;

    private AdditionalResourcesDeployed(String url, Path file, TestsStatus testsStatus) {
        this.url = url;
        this.file = file;
        this.testsStatus = testsStatus;
    }

    static AdditionalResourcesDeployed deploy(AdditionalResources annotation, TestsStatus testsStatus,
                                              OpenShiftClient oc, AwaitUtil awaitUtil) throws IOException, InterruptedException {
        String url = annotation.value();

        Path tempFile = copyResourceIntoTempFile(url);
        ImageOverrides.apply(tempFile, oc);

        System.out.println(ansi().a("deploying ").fgYellow().a(url).reset());
        new Command("oc", "apply", "-f", tempFile.toString()).runAndWait();

        List<HasMetadata> deployedResources = oc.load(Files.newInputStream(tempFile)).get();
        awaitUtil.awaitReadiness(deployedResources);

        return new AdditionalResourcesDeployed(url, tempFile, testsStatus);
    }

    private static Path copyResourceIntoTempFile(String url) throws IOException, MalformedURLException {
        try (InputStream resources = openResource(url)) {
            Path tempFile = Files.createTempFile("additional-resources", ".yml");
            Files.copy(resources, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }

    private static InputStream openResource(String url) throws IOException, MalformedURLException {
        if (url.startsWith("classpath:")) {
            String classloaderResource = url.substring("classpath:".length());
            return OpenShiftTestExtension.class.getClassLoader().getResourceAsStream(classloaderResource);
        }

        return new URL(url).openStream();
    }

    @Override
    public void close() throws Throwable {
        if (RetainOnFailure.isEnabled() && testsStatus.failed) {
            return;
        }

        System.out.println(ansi().a("undeploying ").fgYellow().a(url).reset());
        new Command("oc", "delete", "-f", file.toString(), "--ignore-not-found").runAndWait();
        Files.delete(file);
    }
}
