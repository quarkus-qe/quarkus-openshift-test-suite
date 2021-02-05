package io.quarkus.ts.openshift.common.deploy;

import java.util.Map;

/**
 * If the test class is using the {@code ManualDeploymentStrategy} strategy, the test framework will delegate the deployment
 * to the test case.
 *
 * @see CustomAppMetadata
 */
public class ManualDeploymentStrategy implements DeploymentStrategy {

    @Override
    public void deploy(Map<String, String> envVars) throws Exception {
        // Do nothing
    }

    @Override
    public void undeploy() throws Exception {
        // Do nothing
    }
}
