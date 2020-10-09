package io.quarkus.ts.openshift.common.util;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.internal.readiness.OpenShiftReadiness;

/**
 * Unifies Fabric8 Kubernetes Client's {@link Readiness} and {@link OpenShiftReadiness}.
 * No other class should refer to them directly; using {@code ReadinessUtil} is preferred.
 */
public class ReadinessUtil {
    public static boolean isReadinessApplicable(Class<? extends HasMetadata> resourceClass) {
        // unfortunately `OpenShiftReadiness` doesn't have a `isReadinessApplicable` method
        return Readiness.isReadinessApplicable(resourceClass)
                || DeploymentConfig.class.isAssignableFrom(resourceClass)
                ;
    }

    public static boolean isReady(HasMetadata resource) {
        // `OpenShiftReadiness.isReady` calls `Readiness.isReady` if it is a Kubernetes resource
        return OpenShiftReadiness.isReady(resource);
    }

    public static boolean isPodReady(Pod pod) {
        return Readiness.isPodReady(pod);
    }
}
