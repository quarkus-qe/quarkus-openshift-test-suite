package io.quarkus.ts.openshift.security.jwt.cookie;

import io.quarkus.ts.openshift.common.OpenShiftTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.fusesource.jansi.Ansi.ansi;

@OpenShiftTest
public class SecurityJwtCookieOpenShiftIT extends AbstractSecurityJwtCookieTest {
    @Test
    @Override
    public void tokenExpirationGracePeriod() throws IOException, GeneralSecurityException {
        try {
            super.tokenExpirationGracePeriod();
        } catch (AssertionError e) {
            System.out.println("################################################################################");
            System.out.println("#                      " + ansi().fgCyan().a("C L O C K   S K E W   W A R N I N G").reset() + "                     #");
            System.out.println("#                                                                              #");
            System.out.println("# The " + ansi().fgYellow().a("tokenExpirationGracePeriod").reset() + " test failed. This often happens when          #");
            System.out.println("# the clocks on your machine are out of sync with the clocks in the OpenShift  #");
            System.out.println("# cluster. If you're on some kind of a VPN, check that the NTP server you use  #");
            System.out.println("# is accessible from inside the VPN, and if not, configure your system to use  #");
            System.out.println("# the NTP server provided inside the VPN.                                      #");
            System.out.println("################################################################################");

            throw e;
        }
    }
}
