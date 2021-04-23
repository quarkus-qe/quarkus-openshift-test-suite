package io.quarkus.ts.openshift.deployment.strategies.quarkus;

import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.deploy.UsingQuarkusPluginDeploymentStrategy;

@OpenShiftTest(strategy = UsingQuarkusPluginDeploymentStrategy.UsingDockerStrategy.class)
public class QuarkusBuildDockerStrategyOpenShiftIT extends QuarkusDeploymentStrategyOpenShiftIT {

}
