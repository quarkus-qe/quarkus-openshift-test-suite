package io.quarkus.ts.openshift.extension;

import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;

@QuarkusTest
public class ExtensionTest {
    private static final String QS = "https://github.com/quarkusio/quarkus-quickstarts.git";

    private static final String PATH_QS = "quarkus-quickstarts/";

    private static final String APP = "getting-started";

    private static final String PATH_APP = PATH_QS + APP + "/";

    private static final String PATH_APP_RESOURCE2CHANGE = PATH_APP + "src/main/java/org/acme/getting/started/GreetingResource.java";

    private static final String PATH_APP_POM = PATH_APP + "pom.xml";

    private static final String PATH_APP_PROPERTIES = PATH_APP + "src/main/resources/application.properties";

    private List<String> properties = Collections.unmodifiableList(
            Arrays.asList(
                    "quarkus.kubernetes-client.trust-certs=true",
                    "quarkus.s2i.base-jvm-image=registry.access.redhat.com/openjdk/openjdk-11-rhel7",
                    "quarkus.openshift.expose=true"
            ));

    @Test
    public void checkAddExtension_QuarkusOpenshift() throws IOException, InterruptedException {
        new Command("git", "clone", QS).runAndWait();
        if (new File(PATH_APP_PROPERTIES).exists()) {
            new Command("rm", "-f", PATH_APP_PROPERTIES).runAndWait();
        }

        // pom has no 'quarkus-openshift' dependency, application.properties does not exist and the app is 'getting-started'
        Assertions.assertFalse(fileContainsKey(PATH_APP_POM, "quarkus-openshift"));
        Assertions.assertFalse(new File(PATH_APP_PROPERTIES).exists());
        Assertions.assertTrue(fileContainsKey(PATH_APP_POM, "<artifactId>" + APP + "</artifactId>"));

        final String projectName = ThreadLocalRandom.current()
                .ints(10, 'a', 'z' + 1)
                .collect(() -> new StringBuilder("ts-"), StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        new Command("oc", "new-project", projectName).runAndWait();
        final OpenShiftClientResource openShiftClientResource = OpenShiftClientResource.createDefault();
        final OpenShiftClient oc = openShiftClientResource.client;
        // apply 'quarkus-openshift' extension and create application.properties
        new Command("./mvnw", "quarkus:add-extension",
                    "-Dversion.quarkus=" + System.getProperty("version.quarkus"),
                    "-Dextensions=openshift", "-f", PATH_APP_POM).runAndWait();
        createApplicationProperties(PATH_APP_PROPERTIES, properties);
        Assertions.assertTrue(fileContainsKey(PATH_APP_POM, "quarkus-openshift"));
        Assertions.assertTrue(new File(PATH_APP_PROPERTIES).exists());

        // run app on OpenShift and check address
        runAppAndVerify(true, oc, projectName);
        Assertions.assertEquals(oc.deploymentConfigs().list().getItems().stream().findFirst().get().getMetadata().getName(), APP,
                                "Deployment config name should equal '" + APP + "'.");
        Assertions.assertEquals(oc.services().list().getItems().stream().findFirst().get().getMetadata().getName(), APP,
                                "Service name should equal '" + APP + "'.");
        Assertions.assertEquals(oc.routes().list().getItems().stream().findFirst().get().getMetadata().getName(), APP,
                                "Route name should equal '" + APP + "'.");

        // redeploy the app and verify modifications
        modifyFileForTesting(PATH_APP_RESOURCE2CHANGE);
        runAppAndVerify(false, oc, projectName);

        new Command("oc", "delete", "project", projectName).runAndWait();
    }

    private void runAppAndVerify(boolean before, OpenShiftClient oc, String projectName) throws IOException, InterruptedException {
        System.out.println(before ? "BEFORE STARTS" : "AFTER STARTS");
        new Command("./mvnw", "clean", "package",
                    "-f", PATH_APP_POM,
                    "-Dquarkus.kubernetes.deploy=true",
                    "-Dversion.quarkus=" + System.getProperty("version.quarkus"),
                    "-DskipTests",
                    "-DskipITs").runAndWait();

        Assertions.assertEquals(projectName, oc.getNamespace());
        new Command("sleep", "40s").runAndWait();
        String url = "http://" + oc.routes().withName(APP).get().getSpec().getHost() + "/hello";
        new Command("curl", url).runAndWait();
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    final String expected = before ? "hello" : "hello after";
                    Assertions.assertEquals(expected,
                                            line,
                                            "Response should contain one row message '" + expected + "'");
                }
            }
        });
        System.out.println(before ? "BEFORE ENDS" : "AFTER ENDS");
    }

    private void modifyFileForTesting(String path) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            List<String> replaced = lines
                    .map(line -> line.replaceAll("\"hello\";", "\"hello after\";"))
                    .collect(Collectors.toList());
            Files.write(Paths.get(path), replaced);
        }
    }

    private void createApplicationProperties(String propPath, List<String> properties) throws IOException {
        FileWriter fw = new FileWriter(propPath, true);
        BufferedWriter bw = new BufferedWriter(fw);
        for (String property : properties) {
            bw.write(property);
            bw.newLine();
        }
        bw.close();
    }

    private boolean fileContainsKey(String path, String key) throws IOException {
        return Files.lines(Paths.get(path)).anyMatch(line -> line.contains(key));
    }
}
