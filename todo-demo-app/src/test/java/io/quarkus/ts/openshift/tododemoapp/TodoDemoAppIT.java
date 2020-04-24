package io.quarkus.ts.openshift.tododemoapp;

import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import io.quarkus.ts.openshift.common.injection.TestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;

@OpenShiftTest
@AdditionalResources("classpath:openjdk-11-rhel7.yaml")
@AdditionalResources("classpath:todo-demo-app.yaml")
public class TodoDemoAppIT {

	@TestResource
	private OpenShiftClient oc;

	@Test
	public void verify() {
		String todoDemoApp = "http://" + oc.routes().withName("todo-demo-app").get().getSpec().getHost();
		when()
				.get(todoDemoApp)
		.then()
				.statusCode(200);
	}

}
