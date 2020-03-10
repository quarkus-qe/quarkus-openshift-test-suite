package io.quarkus.ts.openshift.common;

import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

    static AdditionalResourcesDeployed deploy(AdditionalResources annotation, TestsStatus testsStatus) throws IOException, InterruptedException {
        String url = annotation.value();

        InputStream resources;
        if (url.startsWith("classpath:")) {
            String classloaderResource = url.substring("classpath:".length());
            resources = OpenShiftTestExtension.class.getClassLoader().getResourceAsStream(classloaderResource);
        } else {
            resources = new URL(url).openStream();
        }
        Path tempFile = Files.createTempFile("additional-resources", null);
        Files.copy(resources, tempFile, StandardCopyOption.REPLACE_EXISTING);

        System.out.println(ansi().a("deploying ").fgYellow().a(url).reset());
        new Command("oc", "apply", "-f", tempFile.toString()).runAndWait();

        return new AdditionalResourcesDeployed(url, tempFile, testsStatus);
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
