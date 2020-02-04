package io.quarkus.ts.openshift.app.metadata;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public final class AppMetadata {
    public final String appName;
    public final String httpRoot;
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
        try {
            Properties props = new Properties();
            props.load(Files.newBufferedReader(file));
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
