package io.quarkus.ts.openshift.lifecycle;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

@QuarkusMain
public class Main {

    public static final String ARGUMENTS_FROM_MAIN = "Received arguments: ";

    private static final Logger LOG = Logger.getLogger(Main.class);

    public static void main(String... args) {
        LOG.info(ARGUMENTS_FROM_MAIN + String.join(",", args));
        Quarkus.run(args);
    }
}
