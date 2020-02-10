package io.quarkus.ts.openshift.common.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test instance field or test method parameter for injection.
 * The field/parameter must have one of these types:
 * <ul>
 *     <li>{@link io.fabric8.openshift.client.OpenShiftClient}</li>
 *     <li>{@link io.quarkus.ts.openshift.app.metadata.AppMetadata}</li>
 *     <li>{@link io.quarkus.ts.openshift.common.util.AwaitUtil}</li>
 *     <li>{@link io.quarkus.ts.openshift.common.util.OpenShiftUtil}</li>
 * </ul>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestResource {
}
