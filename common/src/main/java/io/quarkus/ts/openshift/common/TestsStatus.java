package io.quarkus.ts.openshift.common;

// single instance of this class is shared for the entire test class
// I couldn't find a better way how to figure out if there was a failure in the AfterAllCallback extension point
final class TestsStatus {
    // TODO visibility? does this need to be `volatile`?
    boolean failed;

    TestsStatus() {
    }
}
