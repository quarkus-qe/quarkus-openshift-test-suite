package io.quarkus.ts.openshift.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Resolves the route endpoint and copies the value into the specified environment variable.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(InjectRouteUrlIntoAppContainer.class)
public @interface InjectRouteUrlIntoApp {
    String route();

    String envVar();
}
