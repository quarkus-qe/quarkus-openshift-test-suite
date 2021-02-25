package io.quarkus.ts.openshift.common.util;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.common.DefaultTimeout;
import io.quarkus.ts.openshift.common.OpenShiftTestException;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.fusesource.jansi.Ansi.ansi;

public final class OpenShiftUtil {
    private final OpenShiftClient oc;
    private final AwaitUtil await;

    public OpenShiftUtil(OpenShiftClient oc, AwaitUtil await) {
        this.oc = oc;
        this.await = await;
    }

    public String getNamespace() {
        return oc.getNamespace();
    }

    public List<Pod> getPods() {
        return oc.pods().list().getItems();
    }

    public List<Pod> listPodsForDeploymentConfig(String deploymentConfigName) {
        return oc.pods()
                .inNamespace(oc.getNamespace())
                .withLabel("deploymentconfig", deploymentConfigName)
                .list()
                .getItems();
    }

    public void scale(String deploymentConfigName, int replicas) {
        scale(deploymentConfigName, replicas, DefaultTimeout.getMinutes());
    }

    public void scale(String deploymentConfigName, int replicas, long timeout) {
        System.out.println(ansi().a("scaling ").fgYellow().a(deploymentConfigName).reset()
                .a(" to ").fgYellow().a(replicas).reset().a(" replica(s)"));

        oc.deploymentConfigs()
                .inNamespace(oc.getNamespace())
                .withName(deploymentConfigName)
                .scale(replicas);

        awaitDeploymentReadiness(deploymentConfigName, replicas, timeout);
    }

    public void deployLatest(String deploymentConfigName, boolean waitForAllReplicas) {
        oc.deploymentConfigs()
                .inNamespace(oc.getNamespace())
                .withName(deploymentConfigName)
                .deployLatest(waitForAllReplicas);
    }

    public void awaitDeploymentReadiness(String deploymentConfigName, int expectedReplicas, long timeout) {
        String waitingReadinessMsg = ansi().a("waiting for ").fgYellow().a(deploymentConfigName).reset()
                .a(" to have exactly ").fgYellow().a(expectedReplicas).reset().a(" ready replica(s)").toString();

        System.out.println(waitingReadinessMsg);

        await().pollInterval(Duration.ofSeconds(1)).atMost(timeout, TimeUnit.MINUTES).until(() -> {
            // ideally, we'd look at deployment config's status.availableReplicas field,
            // but that's only available since OpenShift 3.5
            List<Pod> pods = listPodsForDeploymentConfig(deploymentConfigName);
            try {
                return pods.size() == expectedReplicas && pods.stream().allMatch(ReadinessUtil::isPodReady);
            } catch (IllegalStateException e) {
                return false;
                // the 'Ready' condition can be missing sometimes, in which case Readiness.isPodReady throws an exception
                // here, we'll swallow that exception in hope that the 'Ready' condition will appear later
            }
        });
    }

    public int countReadyReplicas(String deploymentConfigName) {
        List<Pod> pods = listPodsForDeploymentConfig(deploymentConfigName);

        int number = 0;
        for (Pod pod : pods) {
            if (ReadinessUtil.isPodReady(pod)) {
                number++;
            }
        }
        return number;
    }

    public void rolloutChanges(String deploymentConfigName) {
        System.out.println(ansi().a("rolling out ").fgYellow().a(deploymentConfigName).reset());

        int replicas = countReadyReplicas(deploymentConfigName);

        // in reality, user would do `oc rollout latest`, but that's hard (racy) to wait for
        // so here, we'll scale down to 0, wait for that, then scale back to original number of replicas and wait again
        scale(deploymentConfigName, 0);
        scale(deploymentConfigName, replicas);

        await.awaitAppRoute();
    }

    public String getUrlFromRoute(String routeName) throws OpenShiftTestException {
        Route route = oc.routes().withName(routeName).get();
        if (route == null) {
            throw new OpenShiftTestException("Couldn't find route '" + routeName + " ' in namespace '" + getNamespace() + "'");
        }

        final String protocol = route.getSpec().getTls() == null ? "http" : "https";
        final String path = route.getSpec().getPath() == null ? "" : route.getSpec().getPath();
        return String.format("%s://%s%s", protocol, route.getSpec().getHost(), path);
    }

    public void applyYaml(File yaml) throws IOException {
        try (InputStream is = new FileInputStream(yaml)) {
            applyYaml(is);
        }
    }

    public void applyYaml(InputStream yaml) {
        oc.load(yaml).createOrReplace();
    }

    public void deleteYaml(File yaml) throws IOException {
        try (InputStream is = new FileInputStream(yaml)) {
            deleteYaml(is);
        }
    }

    public void deleteYaml(InputStream yaml) {
        oc.load(yaml).delete();
    }

    public String execOnPod(String namespace, String podName, String containerId, String... input)
            throws InterruptedException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CountDownLatch execLatch = new CountDownLatch(1);

        try (ExecWatch execWatch = oc.pods().inNamespace(namespace).withName(podName).inContainer(containerId)
                .writingOutput(out)
                .usingListener(new ExecListener() {
                    @Override
                    public void onOpen(Response response) {
                        // do nothing
                    }

                    @Override
                    public void onFailure(Throwable throwable, Response response) {
                        execLatch.countDown();
                    }

                    @Override
                    public void onClose(int i, String s) {
                        execLatch.countDown();
                    }
                })
                .exec(input)) {
            execLatch.await(5, TimeUnit.MINUTES);
            return out.toString();
        }
    }
}
