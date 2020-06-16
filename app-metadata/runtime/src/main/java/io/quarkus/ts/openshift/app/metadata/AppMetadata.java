package io.quarkus.ts.openshift.app.metadata;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Useful information about the test application.
 * @see #appName
 * @see #httpRoot
 * @see #knownEndpoint
 */
public final class AppMetadata {
    /**
     * Name of the application, which is used as the name of Kubernetes resources.
     * If not customized via {@code quarkus.application.name} or {@code quarkus.container-image.name},
     * defaults to the name of the artifact.
     */
    public final String appName;
    /**
     * Root path for the HTTP endpoints exposed by the application.
     * If not customized via {@code quarkus.http.root-path}, defaults to {@code /}.
     */
    public final String httpRoot;
    /**
     * URL path to some known endpoint that the application exposes.
     * If the application provides a readiness probe, its path is used.
     * Otherwise, if the application provides a liveness probe, its path is used.
     * Otherwise, {@code /} is used.
     */
    public final String knownEndpoint;

    public AppMetadata(String appName, String httpRoot, String knownEndpoint) {
        this.appName = appName;
        this.httpRoot = httpRoot;
        this.knownEndpoint = knownEndpoint;
    }

    @Override
    public String toString() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("app-name", appName);
        data.put("http-root", httpRoot);
        data.put("known-endpoint", knownEndpoint);
        return data.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue() + "\n")
                .collect(Collectors.joining());
    }

    public static AppMetadata load(Path file) {
        Properties props = new Properties();
        try (Reader fileReader = Files.newBufferedReader(file)) {
            props.load(fileReader);
            return new AppMetadata(
                    props.getProperty("app-name"),
                    props.getProperty("http-root"),
                    props.getProperty("known-endpoint")
            );
        } catch (NoSuchFileException e) {
            throw new RuntimeException(file + " not found, did you add the app-metadata extension?", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
