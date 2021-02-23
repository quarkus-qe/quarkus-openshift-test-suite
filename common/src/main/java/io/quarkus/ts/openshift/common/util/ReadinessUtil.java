package io.quarkus.ts.openshift.common.util;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.fabric8.openshift.client.internal.readiness.OpenShiftReadiness;

/**
 * Unifies Fabric8 Kubernetes Client's {@link Readiness} and {@link OpenShiftReadiness}.
 * No other class should refer to them directly; using {@code ReadinessUtil} is preferred.
 */
public final class ReadinessUtil {
    private static final OpenShiftReadinessAccess ACCESS = new OpenShiftReadinessAccess();

    public static boolean isReady(HasMetadata resource) {
        return ACCESS.isReady(resource);
    }

    public static boolean isReadinessApplicable(HasMetadata resource) {
        return ACCESS.isReadinessApplicable(resource);
    }

    // ---

    private static final class OpenShiftReadinessAccess extends OpenShiftReadiness {
        @Override
        public boolean isReadinessApplicable(HasMetadata item) {
            return super.isReadinessApplicable(item);
        }
    }
}