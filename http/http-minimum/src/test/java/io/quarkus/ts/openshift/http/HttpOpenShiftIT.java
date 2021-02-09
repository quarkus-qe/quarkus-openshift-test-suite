package io.quarkus.ts.openshift.http;

import io.quarkus.ts.openshift.common.OnlyIfConfigured;
import io.quarkus.ts.openshift.common.OpenShiftTest;

@OpenShiftTest
@OnlyIfConfigured("ts.authenticated-registry")
public class HttpOpenShiftIT extends AbstractHttpTest{ }
