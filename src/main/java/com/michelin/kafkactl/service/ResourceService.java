/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.michelin.kafkactl.service;

import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECTOR;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECT_CLUSTER;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONSUMER_GROUP_RESET_OFFSET_RESPONSE;
import static com.michelin.kafkactl.util.constant.ResourceKind.DELETE_RECORDS_RESPONSE;
import static com.michelin.kafkactl.util.constant.ResourceKind.SUBJECT;
import static com.michelin.kafkactl.util.constant.ResourceKind.VAULT_RESPONSE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.NamespacedResourceClient;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Output;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.model.SchemaCompatibility;
import com.michelin.kafkactl.util.ResourceDependencySorter;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

/** Resource service. */
@Singleton
public class ResourceService {
    public static final String REFERENCES = "references";
    public static final String SCHEMA = "schema";
    public static final String SCHEMA_FILE = "schemaFile";

    public static final String NAMESPACE_KIND = "Namespace";
    public static final String ROLE_BINDING_KIND = "RoleBinding";
    public static final String ACL_KIND = "AccessControlEntry";
    public static final String SCHEMA_KIND = "Schema";
    private static final String OTHER_KIND = "Other";

    @Inject
    @ReflectiveAccess
    private NamespacedResourceClient namespacedClient;

    @Inject
    @ReflectiveAccess
    private ClusterResourceClient nonNamespacedClient;

    @Inject
    @ReflectiveAccess
    private LoginService loginService;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Inject
    @ReflectiveAccess
    private FileService fileService;

    @Inject
    @ReflectiveAccess
    private ApiResourcesService apiResourcesService;

    /**
     * List all resources of the given types.
     *
     * @param apiResources The resource type
     * @param namespace The namespace
     * @param search The search param to filter resources
     * @param resourceName The resource name
     * @param output The output format
     * @param commandSpec The command that triggered the action
     * @return A map of resource type and list of resources
     */
    public int list(
            List<ApiResource> apiResources,
            String namespace,
            String resourceName,
            Map<String, String> search,
            Output output,
            CommandSpec commandSpec) {
        // Get a single kind of resources
        if (apiResources.size() == 1) {
            try {
                List<Resource> resources =
                        listResourcesWithType(apiResources.getFirst(), namespace, resourceName, search);
                if (!resources.isEmpty()) {
                    formatService.displayList(resources.getFirst().getKind(), resources, output, commandSpec);
                } else {
                    formatService.displayNoResource(apiResources, search, resourceName, commandSpec);
                }
                return 0;
            } catch (HttpClientResponseException exception) {
                formatService.displayError(exception, commandSpec);
                return 1;
            }
        }

        // Get all
        int errors = apiResources.stream()
                .map(apiResource -> {
                    try {
                        List<Resource> resources = listResourcesWithType(apiResource, namespace, resourceName, null);
                        if (!resources.isEmpty()) {
                            formatService.displayList(resources.getFirst().getKind(), resources, output, commandSpec);
                        }
                        return 0;
                    } catch (HttpClientResponseException exception) {
                        formatService.displayError(exception, apiResource.getKind(), resourceName, commandSpec);
                        return 1;
                    }
                })
                .mapToInt(value -> value)
                .sum();

        return errors > 0 ? 1 : 0;
    }

    /**
     * List all resources of given type.
     *
     * @param apiResource The resource type
     * @param namespace The namespace
     * @param resourceName The resource name
     * @param search The resource search parameters mapping
     * @return A list of resources
     */
    public List<Resource> listResourcesWithType(
            ApiResource apiResource, String namespace, String resourceName, Map<String, String> search) {
        Map<String, String> queryParam = new HashMap<>();
        if (search != null) {
            queryParam.putAll(search);
        }
        queryParam.put("name", resourceName);

        return apiResource.isNamespaced()
                ? namespacedClient.list(namespace, apiResource.getPath(), resourceName, loginService.getAuthorization())
                : nonNamespacedClient.list(loginService.getAuthorization(), apiResource.getPath(), queryParam);
    }

    /**
     * Get a resource by type and name.
     *
     * @param apiResource The resource type
     * @param namespace The namespace
     * @param resourceName The resource name
     * @param throwError true if error should be thrown
     * @return A resource
     */
    public Resource getSingleResourceWithType(
            ApiResource apiResource, String namespace, String resourceName, boolean throwError) {
        HttpResponse<Resource> response = apiResource.isNamespaced()
                ? namespacedClient.get(namespace, apiResource.getPath(), resourceName, loginService.getAuthorization())
                : nonNamespacedClient.get(loginService.getAuthorization(), apiResource.getPath(), resourceName);

        // Micronaut does not throw exception on 404, so produce a 404 manually
        if (response.getStatus().equals(HttpStatus.NOT_FOUND) && throwError) {
            throw new HttpClientResponseException(response.reason(), response);
        }

        return response.body();
    }

    /**
     * Apply a given resource.
     *
     * @param apiResource The resource type
     * @param namespace The namespace
     * @param resource The resource
     * @param dryRun Is dry run mode or not?
     * @param commandSpec The command that triggered the action
     * @return An HTTP response
     */
    public HttpResponse<Resource> apply(
            ApiResource apiResource, String namespace, Resource resource, boolean dryRun, CommandSpec commandSpec) {
        try {
            HttpResponse<Resource> response = apiResource.isNamespaced()
                    ? namespacedClient.apply(
                            namespace, apiResource.getPath(), loginService.getAuthorization(), resource, dryRun)
                    : nonNamespacedClient.apply(
                            loginService.getAuthorization(), apiResource.getPath(), resource, dryRun);

            commandSpec
                    .commandLine()
                    .getOut()
                    .println(formatService.prettifyKind(response.body().getKind())
                            + " \"" + response.body().getMetadata().getName() + "\""
                            + (response.header("X-Ns4kafka-Result") != null
                                    ? " " + response.header("X-Ns4kafka-Result")
                                    : "")
                            + ".");
            return response;
        } catch (HttpClientResponseException e) {
            formatService.displayError(
                    e, resource.getKind(), resource.getMetadata().getName(), commandSpec);
            return null;
        }
    }

    /**
     * Delete a given resource.
     *
     * @param apiResource The resource type
     * @param namespace The namespace
     * @param name The resource name or wildcard
     * @param version The version of the resource, for schemas only
     * @param dryRun Is dry run mode or not?
     * @param commandSpec The command that triggered the action
     * @return true if deletion succeeded, false otherwise
     */
    public boolean delete(
            ApiResource apiResource,
            String namespace,
            String name,
            @Nullable String version,
            boolean dryRun,
            CommandSpec commandSpec) {
        try {
            HttpResponse<List<Resource>> response = apiResource.isNamespaced()
                    ? namespacedClient.delete(
                            namespace, apiResource.getPath(), loginService.getAuthorization(), name, version, dryRun)
                    : nonNamespacedClient.delete(loginService.getAuthorization(), apiResource.getPath(), name, dryRun);

            // Micronaut does not throw exception on 404, so produce a 404 manually
            if (response.getStatus().equals(HttpStatus.NOT_FOUND)) {
                throw new HttpClientResponseException(response.reason(), response);
            }

            List<String> resourceNames = response.body().stream()
                    .map(deletionResponse -> deletionResponse.getMetadata().getName())
                    .toList();

            resourceNames.forEach(resourceName -> commandSpec
                    .commandLine()
                    .getOut()
                    .println(formatService.prettifyKind(apiResource.getKind()) + " \"" + resourceName + "\""
                            + (version != null ? " version " + version : "") + " deleted."));

            return true;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, apiResource.getKind(), name, commandSpec);
            return false;
        }
    }

    /**
     * Import all resources of given types.
     *
     * @param apiResources The resource types
     * @param namespace The namespace
     * @param dryRun Is dry run mode or not?
     * @param commandSpec The command that triggered the action
     * @return 0 if the command succeed, 1 otherwise
     */
    public int importAll(List<ApiResource> apiResources, String namespace, boolean dryRun, CommandSpec commandSpec) {
        int errors = apiResources.stream()
                .map(apiResource -> {
                    try {
                        List<Resource> resources = namespacedClient.importResources(
                                namespace, apiResource.getPath(), loginService.getAuthorization(), dryRun);
                        if (!resources.isEmpty()) {
                            formatService.displayList(apiResource.getKind(), resources, TABLE, commandSpec);
                        } else {
                            commandSpec
                                    .commandLine()
                                    .getOut()
                                    .println("No " + apiResource.getKind().toLowerCase() + " to import.");
                        }
                        return 0;
                    } catch (HttpClientResponseException e) {
                        formatService.displayError(e, commandSpec);
                        return 1;
                    }
                })
                .mapToInt(value -> value)
                .sum();

        return errors > 0 ? 1 : 0;
    }

    /**
     * Delete records for a given topic.
     *
     * @param namespace The namespace
     * @param topic The topic to delete records
     * @param dryRun Is dry run mode or not?
     * @param commandSpec The command that triggered the action
     * @return The deleted records response
     */
    public int deleteRecords(String namespace, String topic, boolean dryRun, CommandSpec commandSpec) {
        try {
            List<Resource> resources =
                    namespacedClient.deleteRecords(loginService.getAuthorization(), namespace, topic, dryRun);
            if (!resources.isEmpty()) {
                formatService.displayList(DELETE_RECORDS_RESPONSE, resources, TABLE, commandSpec);
            } else {
                commandSpec.commandLine().getOut().println("No record to delete.");
            }
            return 0;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }

    /**
     * Reset offsets for a given topic and consumer group.
     *
     * @param namespace The namespace
     * @param group The consumer group
     * @param resource The information about how to reset
     * @param dryRun Is dry run mode or not?
     * @param commandSpec The command that triggered the action
     * @return 0 if the command succeeded, 1 otherwise
     */
    public int resetOffsets(
            String namespace, String group, Resource resource, boolean dryRun, CommandSpec commandSpec) {
        try {
            List<Resource> resources =
                    namespacedClient.resetOffsets(loginService.getAuthorization(), namespace, group, resource, dryRun);
            if (!resources.isEmpty()) {
                formatService.displayList(CONSUMER_GROUP_RESET_OFFSET_RESPONSE, resources, TABLE, commandSpec);
            } else {
                commandSpec.commandLine().getOut().println("No offset to reset.");
            }
            return 0;
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, commandSpec);
            return 1;
        }
    }

    /**
     * Change the state of a given connector.
     *
     * @param namespace The namespace
     * @param connector The connector name
     * @param changeConnectorState The state
     * @param commandSpec The command that triggered the action
     * @return The resource
     */
    public Optional<Resource> changeConnectorState(
            String namespace, String connector, Resource changeConnectorState, CommandSpec commandSpec) {
        try {
            HttpResponse<Resource> response = namespacedClient.changeConnectorState(
                    namespace, connector, changeConnectorState, loginService.getAuthorization());

            // Micronaut does not throw exception on 404, so produce a 404 manually
            if (response.getStatus().equals(HttpStatus.NOT_FOUND)) {
                throw new HttpClientResponseException(response.reason(), response);
            }
            return response.getBody();
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, CONNECTOR, connector, commandSpec);
            return Optional.empty();
        }
    }

    /**
     * Change the compatibility of a given schema.
     *
     * @param namespace The namespace
     * @param subject The schema subject
     * @param compatibility The compatibility to apply
     * @param commandSpec The command that triggered the action
     * @return The resource
     */
    public Optional<Resource> changeSchemaCompatibility(
            String namespace, String subject, SchemaCompatibility compatibility, CommandSpec commandSpec) {
        try {
            HttpResponse<Resource> response = namespacedClient.changeSchemaCompatibility(
                    namespace, subject, Map.of("compatibility", compatibility.name()), loginService.getAuthorization());

            // Micronaut does not throw exception on 404, so produce a 404 manually
            if (response.getStatus().equals(HttpStatus.NOT_FOUND)) {
                throw new HttpClientResponseException(response.reason(), response);
            }

            return response.getBody();
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, SUBJECT, subject, commandSpec);
            return Optional.empty();
        }
    }

    /**
     * Reset user password.
     *
     * @param namespace The namespace
     * @param user The user
     * @param output The output format
     * @param commandSpec The command that triggered the action
     * @return 0 if the command succeeded, 1 otherwise
     */
    public int resetPassword(String namespace, String user, Output output, CommandSpec commandSpec) {
        try {
            HttpResponse<Resource> response =
                    namespacedClient.resetPassword(namespace, user, loginService.getAuthorization());

            // Micronaut does not throw exception on 404, so produce a 404 manually
            if (response.getStatus().equals(HttpStatus.NOT_FOUND)) {
                throw new HttpClientResponseException(response.reason(), response);
            }

            formatService.displaySingle(response.body(), output, commandSpec);
            return 0;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }

    /**
     * List all available connect clusters for vaulting.
     *
     * @param namespace The namespace
     * @param commandSpec The command that triggered the action
     * @return 0 if the command succeeded, 1 otherwise
     */
    public int listAvailableVaultsConnectClusters(String namespace, CommandSpec commandSpec) {
        try {
            List<Resource> availableConnectClusters =
                    namespacedClient.listAvailableVaultsConnectClusters(namespace, loginService.getAuthorization());
            if (!availableConnectClusters.isEmpty()) {
                formatService.displayList(CONNECT_CLUSTER, availableConnectClusters, TABLE, commandSpec);
            } else {
                commandSpec.commandLine().getOut().println("No connect cluster configured as vault.");
            }

            return 0;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }

    /**
     * Vault a list of passwords for a specific Kafka Connect Cluster.
     *
     * @param namespace The namespace
     * @param connectCluster The Kafka connect cluster to use for vaulting secret.
     * @param passwords The list of passwords to encrypt.
     * @param commandSpec The command that triggered the action
     * @return 0 if the command succeeded, 1 otherwise
     */
    public int vaultsOnConnectClusters(
            final String namespace,
            final String connectCluster,
            final List<String> passwords,
            CommandSpec commandSpec) {
        try {
            List<Resource> results = namespacedClient.vaultsOnConnectClusters(
                    namespace, connectCluster, passwords, loginService.getAuthorization());
            formatService.displayList(VAULT_RESPONSE, results, TABLE, commandSpec);
            return 0;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }

    /**
     * Parse resources in given directory/file.
     *
     * @param file The directory/file to parse
     * @param recursive Explore given directory recursively or not ?
     * @param commandSpec The command that triggered the action
     * @return The list of resources
     */
    public List<Resource> parseResources(Optional<File> file, boolean recursive, CommandSpec commandSpec) {
        if (file.isPresent()) {
            // List all files to process
            List<File> yamlFiles = fileService.computeYamlFileList(file.get(), recursive);
            if (yamlFiles.isEmpty()) {
                throw new ParameterException(
                        commandSpec.commandLine(),
                        "Could not find YAML or YML files in " + file.get().getName() + " directory.");
            }
            // Load each files
            return fileService.parseResourceListFromFiles(yamlFiles);
        }

        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\\Z");
        return fileService.parseResourceListFromString(scanner.next());
    }

    /**
     * Validate if the resources are allowed on the server for the current user. If not, throw an exception.
     *
     * @param resources The resources to validate
     * @param commandSpec The command that triggered the action
     */
    public void validateAllowedResources(List<Resource> resources, CommandSpec commandSpec) {
        List<Resource> notAllowedResourceTypes = apiResourcesService.filterNotAllowedResourceTypes(resources);
        if (!notAllowedResourceTypes.isEmpty()) {
            String kinds = notAllowedResourceTypes.stream()
                    .map(Resource::getKind)
                    .distinct()
                    .collect(Collectors.joining(", "));
            throw new ParameterException(
                    commandSpec.commandLine(), "The server does not have resource type(s) " + kinds + ".");
        }
    }

    /**
     * Sort resources following this order: 1. Namespace resources, 2. ACL and RoleBinding, 3. Connector,
     * ConnectCluster, KafkaStreams and Schemas. Schemas are further ordered according to their dependencies (aka
     * references), specified by the fully qualified names of Schemas.
     *
     * @param resources The list of schema to sort
     * @param commandSpec The command that triggered the action
     * @return A sorted list of resources
     */
    public List<Resource> prepareResources(List<Resource> resources, CommandLine.Model.CommandSpec commandSpec) {
        Map<String, List<Resource>> resourcesByKind = resources.stream()
                .collect(Collectors.groupingBy(r -> List.of(NAMESPACE_KIND, ROLE_BINDING_KIND, ACL_KIND, SCHEMA_KIND)
                                .contains(r.getKind())
                        ? r.getKind()
                        : OTHER_KIND));

        List<Resource> sortedSchemaResources =
                prepareSchemaResources(resourcesByKind.getOrDefault(SCHEMA_KIND, List.of()), commandSpec);
        List<Resource> allResources = new ArrayList<>();
        Stream.of(
                        resourcesByKind.getOrDefault(NAMESPACE_KIND, List.of()),
                        resourcesByKind.getOrDefault(ROLE_BINDING_KIND, List.of()),
                        resourcesByKind.getOrDefault(ACL_KIND, List.of()),
                        sortedSchemaResources,
                        resourcesByKind.getOrDefault(OTHER_KIND, List.of()))
                .forEach(allResources::addAll);
        return allResources;
    }

    private List<Resource> prepareSchemaResources(List<Resource> schemaResources, CommandSpec commandSpec) {
        Map<String, Resource> nameToResource = new HashMap<>();
        Map<String, Set<String>> dependencies = new HashMap<>();
        List<Resource> nofqNameResources = new ArrayList<>();
        List<Resource> sortedResources = new ArrayList<>();
        for (Resource resource : schemaResources) {
            resource.getSpec().put(SCHEMA, getSchemaContent(resource, commandSpec));
            String fqName = extractFullyQualifiedName(resource, commandSpec);
            if (fqName != null) {
                nameToResource.put(fqName, resource);
                dependencies.put(fqName, getSchemaReferences(resource));
            } else {
                nofqNameResources.add(resource);
            }
        }
        List<String> sortedSchemaNames =
                ResourceDependencySorter.sortResourceNamesByDependencies(nameToResource.keySet(), dependencies);
        for (String fqName : sortedSchemaNames) {
            sortedResources.add(nameToResource.get(fqName));
        }

        sortedResources.addAll(nofqNameResources);
        return sortedResources;
    }

    private static Set<String> getSchemaReferences(Resource resource) {
        Set<String> refs = new HashSet<>();
        Object referencesObj = resource.getSpec().get(REFERENCES);
        if (!(referencesObj instanceof List<?>)) {
            return refs;
        }
        for (Object refObj : (List<?>) referencesObj) {
            if (refObj instanceof Map<?, ?> refMap) {
                Object nameObj = refMap.get("name");
                if (nameObj instanceof String name && name.contains(".")) {
                    refs.add(name);
                }
            }
        }
        return refs;
    }

    private static String getSchemaContent(Resource resource, CommandSpec commandSpec) {
        if (StringUtils.isNotEmpty((CharSequence) resource.getSpec().get(SCHEMA))) {
            return resource.getSpec().get(SCHEMA).toString();
        }
        try {
            return Files.readString(new File(resource.getSpec().get(SCHEMA_FILE).toString()).toPath());
        } catch (Exception e) {
            throw new ParameterException(
                    commandSpec.commandLine(),
                    "Cannot open schema file " + resource.getSpec().get(SCHEMA_FILE)
                            + ". Schema path must be relative to the CLI.");
        }
    }

    private static String extractFullyQualifiedName(Resource resource, CommandSpec commandSpec) {
        try {
            JsonNode node = new ObjectMapper().readTree(getSchemaContent(resource, commandSpec));
            if (node.isArray()) return null; // if schema is a union
            String name = node.has("name") ? node.get("name").asText() : null;
            String ns = node.has("namespace") ? node.get("namespace").asText() : null;
            return ns != null ? ns + "." + name : name;
        } catch (Exception e) {
            return null;
        }
    }
}
