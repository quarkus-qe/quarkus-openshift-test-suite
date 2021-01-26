package io.quarkus.ts.openshift.common.deploy;

/**
 * Interface to deploy a Quarkus application into OpenShift.
 */
public interface DeploymentStrategy {

    /**
     * Deploy action
     */
    void deploy() throws Exception;

    /**
     * Undeploy action
     */
    void undeploy() throws Exception;
}
