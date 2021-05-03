package io.quarkus.ts.openshift.lifecycle;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;

@OpenShiftTest
public class LifecycleApplicationOpenShiftIT {

    private static final String LOGGING_PROPERTY = "-Djava.util.logging.manager=org.jboss.logmanager.LogManager";
    private static final String RUNTIME_LABEL = "app.openshift.io/runtime";
    private static final String RUNTIME_QUARKUS = "quarkus";
    private static final List<String> EXPECTED_ARGUMENTS = Arrays.asList("ARG1", "ARG2");

    @TestResource
    private OpenShiftClient openshiftClient;

    @Test
    public void shouldArgumentsNotContainLoggingProperty() {
        String actualArguments = when().get("/args")
                .then().statusCode(200).extract().asString();

        assertFalse(StringUtils.contains(actualArguments, LOGGING_PROPERTY),
                "Actual arguments contain unexpected properties: " + actualArguments);
        // Can't provide arguments to Java command: https://github.com/quarkusio/quarkus/pull/15670#issuecomment-800857624
        // assertExpectedArguments(actualArguments);
    }

    @Test
    public void shouldPrintMessagesFromQuarkusMain() {
        PodList pods = openshiftClient.pods().withLabel(RUNTIME_LABEL, RUNTIME_QUARKUS).list();
        assertEquals(1, pods.getItems().size(), "Found more than one pod with Quarkus runtime");

        String actualLog = openshiftClient.pods().withName(pods.getItems().get(0).getMetadata().getName()).getLog();
        Optional<String> argumentsLineOpt = toLines(actualLog).filter(line -> line.contains(Main.ARGUMENTS_FROM_MAIN))
                .findFirst();

        assertTrue(argumentsLineOpt.isPresent(),
                "Pod log does not contain the received arguments. Actual content: " + actualLog);
        String argumentsLine = argumentsLineOpt.get();
        assertFalse(argumentsLine.contains(LOGGING_PROPERTY),
                "Pod log contain unexpected properties. Actual content: " + argumentsLine);
        // Can't provide arguments to Java command: https://github.com/quarkusio/quarkus/pull/15670#issuecomment-800857624
        // assertExpectedArguments(argumentsLine);
    }

    private void assertExpectedArguments(String actualArguments) {
        EXPECTED_ARGUMENTS.forEach(arg -> assertTrue(actualArguments.contains(arg),
                "Expected argument " + arg + " was not found in actual arguments: " + actualArguments));
    }

    private static final Stream<String> toLines(String str) {
        return new BufferedReader(new StringReader(str)).lines();
    }

}
