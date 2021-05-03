package io.quarkus.ts.openshift.common.actions;

import java.io.File;

import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.ts.openshift.common.Command;
import io.quarkus.ts.openshift.common.config.Config;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;

public class CopyLogsOnOpenShiftFailureActionImpl implements OnOpenShiftFailureAction {

    private static final String INSTANCES_LOGS_OUTPUT_DIRECTORY = "ts.instance-logs";
    private static final String DEFAULT_LOG_OUTPUT_DIRECTORY = "target/logs";
    private static final String LOG_SUFFIX = ".log";

    @TestResource
    private OpenShiftUtil openShiftUtil;

    @TestResource
    private Config config;

    @Override
    public void execute() throws Exception {
        String namespace = openShiftUtil.getNamespace();
        for (Pod pod : openShiftUtil.getPods()) {
            String podName = pod.getMetadata().getName();
            new Command("oc", "logs", podName).outputToFile(getOutputFile(podName, namespace)).runAndWait();
        }
    }

    private String getOutputFolder() {
        return config.getAsString(INSTANCES_LOGS_OUTPUT_DIRECTORY, DEFAULT_LOG_OUTPUT_DIRECTORY);
    }

    private File getOutputFile(String name, String customLogFolderName) {
        File outputDirectory = new File(getOutputFolder(), customLogFolderName);
        outputDirectory.mkdirs();
        return new File(outputDirectory, name + LOG_SUFFIX);
    }

}
