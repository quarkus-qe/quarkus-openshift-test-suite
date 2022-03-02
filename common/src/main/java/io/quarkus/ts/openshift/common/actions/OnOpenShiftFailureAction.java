package io.quarkus.ts.openshift.common.actions;

/**
 * Interface to perform an action after an OpenShift failure.
 */
public interface OnOpenShiftFailureAction {

    /**
     * Action to perform
     *
     * @throws Exception to be logged
     */
    void execute() throws Exception;
}
