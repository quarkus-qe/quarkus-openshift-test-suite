package io.quarkus.ts.openshift.common.actions;

import io.quarkus.ts.openshift.common.Command;

public class PrintStatusOnOpenShiftFailureActionImpl implements OnOpenShiftFailureAction {

    @Override
    public void execute() throws Exception {
        new Command("oc", "status", "--suggest").runAndWait();
    }

}
