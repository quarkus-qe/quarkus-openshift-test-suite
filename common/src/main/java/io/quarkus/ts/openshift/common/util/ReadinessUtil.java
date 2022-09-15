package io.quarkus.ts.openshift.common.util;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.readiness.OpenShiftReadiness;

/**
 * Unifies Fabric8 Kubernetes Client's {@link Readiness} and {@link OpenShiftReadiness}.
 * No other class should refer to them directly; using {@code ReadinessUtil} is preferred.
 */
public final class ReadinessUtil {
    private static final OpenShiftReadiness ACCESS = new OpenShiftReadiness();

    public static boolean isReady(HasMetadata resource) {
        return ACCESS.isReady(resource);
    }

    /**
     * Workaround: Quarkus 1.12.1.Final (not released yet) upgraded the Fabric8 dependency to 5.1.0 which entered a breaking
     * change in the next commit:
     * https://github.com/fabric8io/kubernetes-client/commit/d8eb90c330403e8ca72bffe962e74c2a4ce9d68a#diff-463619f17f69ff1bb425b571477c5bad6d786afaf2c974a7713c5982242edb79.
     *
     * The breaking change is due to isReadinessApplicable which changed from being a static method to protected, and also
     * changed the method argument.
     *
     * Therefore, in order to make the Test Suite compatible with all Quarkus versions, we copied the method directly here.
     */
    public static boolean isReadinessApplicable(Class<? extends HasMetadata> itemClass) {
        return Deployment.class.isAssignableFrom(itemClass)
                || io.fabric8.kubernetes.api.model.extensions.Deployment.class.isAssignableFrom(itemClass)
                || ReplicaSet.class.isAssignableFrom(itemClass)
                || Pod.class.isAssignableFrom(itemClass)
                || ReplicationController.class.isAssignableFrom(itemClass)
                || Endpoints.class.isAssignableFrom(itemClass)
                || Node.class.isAssignableFrom(itemClass)
                || StatefulSet.class.isAssignableFrom(itemClass)
                || DeploymentConfig.class.isAssignableFrom(itemClass);
    }
}