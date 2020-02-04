package io.quarkus.ts.openshift.common;

public class OpenShiftTestException extends Exception {
    public OpenShiftTestException(String message) {
        super(message);
    }

    public OpenShiftTestException(String message, Throwable cause) {
        super(message, cause);
    }
}
