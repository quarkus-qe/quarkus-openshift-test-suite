package io.quarkus.ts.openshift.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides URL of additional resources (in YAML or JSON format) that shall be deployed before / undeployed after
 * the tested application. When multiple {@code AdditionalResources} annotations are present, deployment order
 * is guaranteed (first before second etc.), but undeployment order is not.
 * <p>
 * In addition to common URL schemes supported by the JDK, an extra {@code classpath:} scheme
 * is recognized, which points to a classloader resource (in that case, the value should <i>not</i> begin
 * with a {@code /}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AdditionalResourcesContainer.class)
public @interface AdditionalResources {
    String value();
}
