package io.quarkus.ts.openshift.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManualDeployment {
    // TODO this is not fully thought through and should be considered very much experimental
    //      for the same reason, it isn't documented in the README

    String appName();

    String httpRoot();

    String knownEndpoint();
}
