/*
 * Copyright 2023 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.deployment;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.LocalPortForward;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.apicurio.deployment.KubernetesTestResources.KEYCLOAK_SERVICE;
import static io.apicurio.deployment.KubernetesTestResources.TEST_NAMESPACE;

public class PortForwardManager implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    KubernetesClient kubernetesClient;
    static LocalPortForward keycloakPortForward;

    private static final Logger logger = LoggerFactory.getLogger(PortForwardManager.class);

    public PortForwardManager() {
        if (Boolean.parseBoolean(System.getProperty("cluster.tests"))) {
            kubernetesClient = new KubernetesClientBuilder()
                    .build();
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (Boolean.parseBoolean(System.getProperty("cluster.tests"))) {
            if (Constants.TEST_PROFILE.equals(Constants.AUTH)) {
                startKeycloakPortForward();
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (Boolean.parseBoolean(System.getProperty("cluster.tests"))) {
            if (Constants.TEST_PROFILE.equals(Constants.AUTH)) {
                if (keycloakPortForward != null) {
                    keycloakPortForward.close();
                }
            }
        }
    }


    private void startKeycloakPortForward() {
        try {
            if (keycloakPortForward != null) {
                keycloakPortForward.close();
            }
            //Create the keycloak port forward so the tests can reach it to get tokens
            keycloakPortForward = kubernetesClient.services()
                    .inNamespace(TEST_NAMESPACE)
                    .withName(KEYCLOAK_SERVICE)
                    .portForward(8090, 8090);
        } catch (IllegalStateException | IOException ex) {
            logger.warn("Error found forwarding keycloak port, the port forwarding might be running already, continuing...", ex);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {

    }
}
