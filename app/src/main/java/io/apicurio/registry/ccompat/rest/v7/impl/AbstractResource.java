/*
 * Copyright 2022 Red Hat
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

package io.apicurio.registry.ccompat.rest.v7.impl;


import io.apicurio.registry.ccompat.dto.SchemaReference;
import io.apicurio.registry.ccompat.rest.error.ConflictException;
import io.apicurio.registry.ccompat.rest.error.UnprocessableEntityException;
import io.apicurio.registry.content.ContentHandle;
import io.apicurio.registry.rest.v2.beans.ArtifactReference;
import io.apicurio.registry.rules.RuleApplicationType;
import io.apicurio.registry.rules.RuleViolationException;
import io.apicurio.registry.rules.RulesService;
import io.apicurio.registry.storage.RegistryStorage;
import io.apicurio.registry.storage.dto.ArtifactMetaDataDto;
import io.apicurio.registry.storage.dto.ArtifactReferenceDto;
import io.apicurio.registry.storage.dto.ArtifactVersionMetaDataDto;
import io.apicurio.registry.storage.dto.StoredArtifactDto;
import io.apicurio.registry.storage.error.ArtifactNotFoundException;
import io.apicurio.registry.storage.error.RuleNotFoundException;
import io.apicurio.registry.storage.error.VersionNotFoundException;
import io.apicurio.registry.types.ArtifactState;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.types.Current;
import io.apicurio.registry.types.RuleType;
import io.apicurio.registry.types.provider.ArtifactTypeUtilProvider;
import io.apicurio.registry.types.provider.ArtifactTypeUtilProviderFactory;
import org.apache.avro.AvroTypeException;
import org.apache.avro.SchemaParseException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.apicurio.registry.storage.RegistryStorage.ArtifactRetrievalBehavior.DEFAULT;

/**
 * @author Carles Arnal
 */
public abstract class AbstractResource {

    @Inject
    Logger log;

    @Inject
    @Current
    RegistryStorage storage;

    @Inject
    RulesService rulesService;

    @Inject
    ApiConverter converter;

    @Inject
    CCompatConfig cconfig;

    @Inject
    ArtifactTypeUtilProviderFactory factory;

    protected ArtifactMetaDataDto createOrUpdateArtifact(String subject, String schema, String artifactType, List<SchemaReference> references, String groupId) {
        ArtifactMetaDataDto res;
        final List<ArtifactReferenceDto> parsedReferences = parseReferences(references, groupId);
        final List<ArtifactReference> artifactReferences = parsedReferences.stream().map(dto -> ArtifactReference.builder().name(dto.getName()).groupId(dto.getGroupId()).artifactId(dto.getArtifactId()).version(dto.getVersion()).build()).collect(Collectors.toList());
        final Map<String, ContentHandle> resolvedReferences = storage.resolveReferences(parsedReferences);
        try {
            ContentHandle schemaContent;
            schemaContent = ContentHandle.create(schema);

            if (!doesArtifactExist(subject, groupId)) {
                rulesService.applyRules(groupId, subject, artifactType, schemaContent, RuleApplicationType.CREATE, artifactReferences, resolvedReferences);
                res = storage.createArtifact(groupId, subject, null, artifactType, schemaContent, parsedReferences);
            } else {
                rulesService.applyRules(groupId, subject, artifactType, schemaContent, RuleApplicationType.UPDATE, artifactReferences, resolvedReferences);
                res = storage.updateArtifact(groupId, subject, null, artifactType, schemaContent, parsedReferences);
            }
        } catch (RuleViolationException ex) {
            if (ex.getRuleType() == RuleType.VALIDITY) {
                throw new UnprocessableEntityException(ex);
            } else {
                throw new ConflictException(ex);
            }
        }
        return res;
    }

    protected ArtifactVersionMetaDataDto lookupSchema(String groupId, String subject, String schema, List<SchemaReference> schemaReferences, String schemaType, boolean normalize) {
        //FIXME simplify logic
        try {
            final String type = schemaType == null ? ArtifactType.AVRO : schemaType;
            final List<ArtifactReferenceDto> artifactReferences = parseReferences(schemaReferences, groupId);
            ArtifactTypeUtilProvider artifactTypeProvider = factory.getArtifactTypeProvider(type);
            ArtifactVersionMetaDataDto amd;

            if (cconfig.canonicalHashModeEnabled.get() || normalize) {
                try {
                    amd = storage.getArtifactVersionMetaData(groupId, subject, true, ContentHandle.create(schema), artifactReferences);
                } catch (ArtifactNotFoundException ex) {
                    if (type.equals(ArtifactType.AVRO)) {
                        //When comparing using content, sometimes the references might be inlined into the content, try to dereference the existing content and compare as a fallback. See https://github.com/Apicurio/apicurio-registry/issues/3588 for more information.
                        //If using this method there is no matching content either, just re-throw the exception.
                        //This approach only works for schema types with dereference support (for now, only Avro in the ccompat API).
                        amd = storage.getArtifactVersions(groupId, subject)
                                .stream().filter(version -> {
                                    StoredArtifactDto artifactVersion = storage.getArtifactVersion(groupId, subject, version);
                                    Map<String, ContentHandle> artifactVersionReferences = storage.resolveReferences(artifactVersion.getReferences());
                                    String dereferencedExistingContentSha = DigestUtils.sha256Hex(artifactTypeProvider.getContentDereferencer().dereference(artifactVersion.getContent(), artifactVersionReferences).content());
                                    return dereferencedExistingContentSha.equals(DigestUtils.sha256Hex(schema));
                                })
                                .findAny()
                                .map(version -> storage.getArtifactVersionMetaData(groupId, subject, version))
                                .orElseThrow(() -> ex);
                    } else {
                        throw ex;
                    }
                }

            } else {
                amd = storage.getArtifactVersionMetaData(groupId, subject, false, ContentHandle.create(schema), artifactReferences);
            }

            return amd;
        } catch (SchemaParseException | AvroTypeException ex) {
            throw new UnprocessableEntityException(ex.getMessage());
        }
    }

    protected Map<String, ContentHandle> resolveReferences(List<SchemaReference> references) {
        Map<String, ContentHandle> resolvedReferences = Collections.emptyMap();
        if (references != null && !references.isEmpty()) {
            //Transform the given references into dtos and set the contentId, this will also detect if any of the passed references does not exist.
            final List<ArtifactReferenceDto> referencesAsDtos = references.stream().map(schemaReference -> {
                final ArtifactReferenceDto artifactReferenceDto = new ArtifactReferenceDto();
                artifactReferenceDto.setArtifactId(schemaReference.getSubject());
                artifactReferenceDto.setVersion(String.valueOf(schemaReference.getVersion()));
                artifactReferenceDto.setName(schemaReference.getName());
                artifactReferenceDto.setGroupId(null);
                return artifactReferenceDto;
            }).collect(Collectors.toList());

            resolvedReferences = storage.resolveReferences(referencesAsDtos);

            if (references.size() > resolvedReferences.size()) {
                //There are unresolvable references, which is not allowed.
                throw new UnprocessableEntityException("Unresolved reference");
            }
        }

        return resolvedReferences;
    }

    protected boolean isArtifactActive(String subject, String groupId, RegistryStorage.ArtifactRetrievalBehavior retrievalBehavior) {
        final ArtifactState state = storage.getArtifactMetaData(groupId, subject, retrievalBehavior).getState();
        return storage.isArtifactExists(groupId, subject) && (state.equals(ArtifactState.ENABLED) || state.equals(ArtifactState.DEPRECATED));
    }

    protected String getLatestArtifactVersionForSubject(String subject, String groupId) {
        try {
            ArtifactMetaDataDto latest = storage.getArtifactMetaData(groupId, subject);
            return latest.getVersion();
        } catch (ArtifactNotFoundException ex) {
            throw new VersionNotFoundException(groupId, subject, "latest");
        }
    }

    protected boolean shouldFilterState(boolean deleted, ArtifactState state) {
        if (deleted) {
            //if deleted is enabled, just return all states
            return true;
        } else {
            return state.equals(ArtifactState.ENABLED);
        }
    }

    protected boolean areAllSchemasDisabled(List<Long> globalIds) {
        return globalIds.stream().anyMatch(globalId -> {
            ArtifactState state = storage.getArtifactMetaData(globalId).getState();
            return state.equals(ArtifactState.DISABLED);
        });
    }

    protected boolean doesArtifactExist(String artifactId, String groupId) {
        try {
            storage.getArtifact(groupId, artifactId, DEFAULT);
            return true;
        } catch (ArtifactNotFoundException ignored) {
            return false;
        }
    }

    protected boolean doesArtifactRuleExist(String artifactId, RuleType type, String groupId) {
        try {
            storage.getArtifactRule(groupId, artifactId, type);
            return true;
        } catch (RuleNotFoundException | ArtifactNotFoundException ignored) {
            return false;
        }
    }

    protected boolean doesGlobalRuleExist(RuleType type) {
        try {
            storage.getGlobalRule(type);
            return true;
        } catch (RuleNotFoundException ignored) {
            return false;
        }
    }

    //Parse references and resolve the contentId. This will fail with ArtifactNotFound if a reference cannot be found.
    protected List<ArtifactReferenceDto> parseReferences(List<SchemaReference> references, String groupId) {
        if (references != null) {
            return references.stream().map(schemaReference -> {
                // Try to get the artifact version.  This will fail if not found with ArtifactNotFound or VersionNotFound
                storage.getArtifactVersionMetaData(groupId, schemaReference.getSubject(), String.valueOf(schemaReference.getVersion()));
                return new ArtifactReferenceDto(groupId, schemaReference.getSubject(), String.valueOf(schemaReference.getVersion()), schemaReference.getName());
            }).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    protected boolean isCcompatManagedType(String artifactType) {
        return artifactType.equals(ArtifactType.AVRO) || artifactType.equals(ArtifactType.PROTOBUF) || artifactType.equals(ArtifactType.JSON);
    }

    /**
     * Given a version string:
     * - if it's a <b>non-negative integer</b>, use that;
     * - if it's a string "latest", find out and use the subject's (artifact's) latest version;
     * - if it's <b>-1</b>, do the same as "latest", even though this behavior is undocumented.
     * See https://github.com/Apicurio/apicurio-registry/issues/2851
     * - otherwise throw an IllegalArgumentException.
     * On success, call the "then" function with the parsed version (MUST NOT be null) and return it's result.
     * Optionally provide an "else" function that will receive the exception that would be otherwise thrown.
     */
    protected  <T> T parseVersionString(String subject, String versionString, String groupId, Function<String, T> then) {
        String version;
        if ("latest".equals(versionString)) {
            version = getLatestArtifactVersionForSubject(subject, groupId);
        } else {
            try {
                var numericVersion = Integer.parseInt(versionString);
                if (numericVersion >= 0) {
                    version = versionString;
                } else if (numericVersion == -1) {
                    version = getLatestArtifactVersionForSubject(subject, groupId);
                } else {
                    throw new ArtifactNotFoundException("Illegal version format: " + versionString);
                }
            } catch (NumberFormatException e) {
                throw new VersionNotFoundException(groupId, subject, versionString);
            }
        }
        return then.apply(version);
    }
}

