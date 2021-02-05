package io.quarkus.ts.openshift.common.deploy;

import java.util.Map;

/**
 * Interface to deploy a Quarkus application into OpenShift.
 */
public interface DeploymentStrategy {

    /**
     * Deploy action
     */
    void deploy(Map<String, String> envVars) throws Exception;

    /**
     * Undeploy action
     */
    void undeploy() throws Exception;
}
