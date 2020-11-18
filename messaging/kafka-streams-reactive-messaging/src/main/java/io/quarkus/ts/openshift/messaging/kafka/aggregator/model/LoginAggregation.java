package io.quarkus.ts.openshift.messaging.kafka.aggregator.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class LoginAggregation {
    public String endpoint;
    public int code;
    public int count;

    public LoginAggregation updateFrom(LoginAttempt loginAttempt) {
        endpoint = loginAttempt.endpoint;
        code = loginAttempt.code;
        count++;

        return this;
    }
}
