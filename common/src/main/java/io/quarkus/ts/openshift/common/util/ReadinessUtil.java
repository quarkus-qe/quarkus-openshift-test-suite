package io.quarkus.ts.openshift.common.util;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.internal.readiness.OpenShiftReadiness;

/**
 * Unifies Fabric8 Kubernetes Client's {@link Readiness} and {@link OpenShiftReadiness}.
 * No other class should refer to them directly; using {@code ReadinessUtil} is preferred.
 * Note: this proxy is strongly couple to `io.fabric8.kubernetes.client.internal.readiness`.
 * We are making a virtual invocation with version 5.1.0 and a static invocation with version < 5.0.X, something
 * that is `quite hacky`. Please consider refactor this proxy, once `Readiness isReadinessApplicable` method become public.
 */
public enum ReadinessUtil {

    INSTANCE;

    private final Readiness readiness;

    ReadinessUtil(){
        readiness = new Readiness();
    }

    public boolean isReadinessApplicable(HasMetadata resource) {
        try{
            return readiness.isReady(resource)|| DeploymentConfig.class.isAssignableFrom(resource.getClass());
        }catch(IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isReady(HasMetadata resource) {
        // `OpenShiftReadiness.isReady` calls `Readiness.isReady` if it is a Kubernetes resource
        return readiness.isReady(resource);
    }

    public boolean isPodReady(Pod pod) {
        return Readiness.isPodReady(pod);
    }
}