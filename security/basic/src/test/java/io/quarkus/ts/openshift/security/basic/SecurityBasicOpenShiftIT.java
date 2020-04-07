package io.quarkus.ts.openshift.security.basic;

import io.quarkus.ts.openshift.common.OpenShiftTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@OpenShiftTest
public class SecurityBasicOpenShiftIT extends AbstractSecurityBasicTest {
}
