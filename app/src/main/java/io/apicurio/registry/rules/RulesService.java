/*
 * Copyright 2020 Red Hat
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

package io.apicurio.registry.rules;

import java.util.List;
import java.util.Map;

import io.apicurio.registry.content.ContentHandle;
import io.apicurio.registry.rest.v2.beans.ArtifactReference;
import io.apicurio.registry.types.RuleType;

/**
 * A service used to apply configured rules to a given content update.  In other words,
 * when artifact content is being created or updated, this service is used to apply
 * any rules configured for the artifact.
 *
 * @author Ales Justin
 * @author Jakub Senko <em>m@jsenko.net</em>
 */
public interface RulesService {

    /**
     * Applies all configured rules to check whether a content update for an artifact is allowed.
     * @param groupId
     * @param artifactId
     * @param artifactType
     * @param artifactContent
     * @param ruleApplicationType
     * @param references
     * @param resolvedReferences
     * @throws RuleViolationException
     */
    public void applyRules(String groupId, String artifactId, String artifactType, ContentHandle artifactContent,
                    RuleApplicationType ruleApplicationType, List<ArtifactReference> references,
                    Map<String, ContentHandle> resolvedReferences) throws RuleViolationException;

    /**
     * Applies a single, specific rule to the content update for the given artifact.
     * @param groupId
     * @param artifactId
     * @param artifactType
     * @param artifactContent
     * @param ruleType
     * @param ruleConfiguration
     * @param ruleApplicationType
     * @param references
     * @param resolvedReferences
     * @throws RuleViolationException
     */
    public void applyRule(String groupId, String artifactId, String artifactType, ContentHandle artifactContent,
                   RuleType ruleType, String ruleConfiguration, RuleApplicationType ruleApplicationType, 
                   List<ArtifactReference> references, Map<String, ContentHandle> resolvedReferences)
            throws RuleViolationException;

    /**
     * Applies configured rules to the content update, relative to ANY artifact version.
     * @param groupId
     * @param artifactId
     * @param artifactVersion
     * @param artifactType
     * @param updatedContent
     * @param references
     * @param resolvedReferences
     * @throws RuleViolationException
     */
    public void applyRules(String groupId, String artifactId, String artifactVersion, String artifactType, 
            ContentHandle updatedContent, List<ArtifactReference> references, Map<String, ContentHandle> resolvedReferences)
            throws RuleViolationException;


    public void applyRulesCompat(String groupId, String artifactId, String artifactVersion, String artifactType,
                                 ContentHandle updatedContent, List<ArtifactReference> references,
                                 Map<String, ContentHandle> resolvedReferences) throws RuleViolationException;
}
