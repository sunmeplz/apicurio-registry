/*
 * Copyright 2021 Red Hat
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

package io.apicurio.registry.limits;

import io.apicurio.common.apps.config.Info;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;

/**
 * @author Fabian Martinez
 */
public class RegistryLimitsConfigurationProducer {

    @Inject
    Logger logger;

    //All limits to -1 , which means by default all limits are disabled

    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-total-schemas")
    @Info(category = "limits", description = "Max total schemas", availableSince = "2.1.0.Final")
    Long defaultMaxTotalSchemas;

    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-schema-size-bytes")
    @Info(category = "limits", description = "Max schema size (bytes)", availableSince = "2.2.3.Final")
    Long defaultMaxSchemaSizeBytes;

    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-artifacts")
    @Info(category = "limits", description = "Max artifacts", availableSince = "2.1.0.Final")
    Long defaultMaxArtifacts;
    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-versions-per-artifact")
    @Info(category = "limits", description = "Max versions per artifacts", availableSince = "2.1.0.Final")
    Long defaultMaxVersionsPerArtifact;

    //TODO content size
    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-artifact-properties")
    @Info(category = "limits", description = "Max artifact properties", availableSince = "2.1.0.Final")
    Long defaultMaxArtifactProperties;
    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-property-key-size")
    @Info(category = "limits", description = "Max artifact property key size", availableSince = "2.1.0.Final")
    Long defaultMaxPropertyKeyBytesSize;
    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-property-value-size")
    @Info(category = "limits", description = "Max artifact property value size", availableSince = "2.1.0.Final")
    Long defaultMaxPropertyValueBytesSize;

    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-artifact-labels")
    @Info(category = "limits", description = "Max artifact labels", availableSince = "2.2.3.Final")
    Long defaultMaxArtifactLabels;
    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-label-size")
    @Info(category = "limits", description = "Max artifact label size", availableSince = "2.1.0.Final")
    Long defaultMaxLabelBytesSize;

    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-name-length")
    @Info(category = "limits", description = "Max artifact name length", availableSince = "2.1.0.Final")
    Long defaultMaxNameLength;
    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-description-length")
    @Info(category = "limits", description = "Max artifact description length", availableSince = "2.1.0.Final")
    Long defaultMaxDescriptionLength;

    @Inject
    @ConfigProperty(defaultValue = "-1", name = "registry.limits.config.max-requests-per-second")
    @Info(category = "limits", description = "Max artifact requests per second", availableSince = "2.2.3.Final")
    Long defaultMaxRequestsPerSecond;


    private boolean isConfigured = true;
    private RegistryLimitsConfiguration defaultLimitsConfiguration;

    @Produces
    @ApplicationScoped
    public RegistryLimitsConfiguration postConstruct() {

        RegistryLimitsConfiguration c = new RegistryLimitsConfiguration();

        c.setMaxTotalSchemasCount(defaultMaxTotalSchemas);
        c.setMaxSchemaSizeBytes(defaultMaxSchemaSizeBytes);
        c.setMaxArtifactsCount(defaultMaxArtifacts);
        c.setMaxVersionsPerArtifactCount(defaultMaxVersionsPerArtifact);

        c.setMaxArtifactPropertiesCount(defaultMaxArtifactProperties);
        c.setMaxPropertyKeySizeBytes(defaultMaxPropertyKeyBytesSize);
        c.setMaxPropertyValueSizeBytes(defaultMaxPropertyValueBytesSize);

        c.setMaxArtifactLabelsCount(defaultMaxArtifactLabels);
        c.setMaxLabelSizeBytes(defaultMaxLabelBytesSize);

        c.setMaxArtifactNameLengthChars(defaultMaxNameLength);
        c.setMaxArtifactDescriptionLengthChars(defaultMaxDescriptionLength);

        c.setMaxRequestsPerSecondCount(defaultMaxRequestsPerSecond);

        defaultLimitsConfiguration = c;

        return defaultLimitsConfiguration;
    }

    public boolean isConfigured() {
        return this.isConfigured;
    }
}
