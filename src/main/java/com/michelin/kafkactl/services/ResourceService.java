package com.michelin.kafkactl.services;

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.NamespacedResourceClient;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.models.SchemaCompatibility;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.*;

@Singleton
public class ResourceService {
    @Inject
    NamespacedResourceClient namespacedClient;

    @Inject
    ClusterResourceClient nonNamespacedClient;

    @Inject
    LoginService loginService;

    @Inject
    FormatService formatService;

    @Inject
    FileService fileService;

    /**
     * List all resources of the given types
     * @param apiResources The resource type
     * @param namespace The namespace
     * @param commandSpec The command that triggered the action
     * @return A map of resource type and list of resources
     */
    public int listAll(List<ApiResource> apiResources, String namespace, CommandLine.Model.CommandSpec commandSpec) {
        // Get a single kind of resources
        if (apiResources.size() == 1) {
            try {
                List<Resource> resources = listResourcesWithType(apiResources.get(0), namespace, commandSpec);
                if (!resources.isEmpty()) {
                    formatService.displayList(resources.get(0).getKind(), resources, TABLE, commandSpec);
                } else {
                    commandSpec.commandLine().getOut().println("No " + apiResources.get(0).getKind().toLowerCase() + " to display.");
                }
                return 0;
            } catch (HttpClientResponseException exception) {
                formatService.displayError(exception, commandSpec);
                return 1;
            }
        }

        // Get all
        int errors = apiResources
                .stream()
                .map(apiResource -> {
                    try {
                        List<Resource> resources = listResourcesWithType(apiResource, namespace, commandSpec);
                        if (!resources.isEmpty()) {
                            formatService.displayList(resources.get(0).getKind(), resources, TABLE, commandSpec);
                        }
                        return 0;
                    } catch (HttpClientResponseException exception) {
                        formatService.displayError(exception, apiResource.getKind(), commandSpec);
                        return 1;
                    }
                })
                .mapToInt(value -> value)
                .sum();

        return errors > 0 ? 1 : 0;
    }

    /**
     * List all resources of given type
     * @param apiResource The resource type
     * @param namespace The namespace
     * @param commandSpec The command that triggered the action
     * @return A list of resources
     */
    public List<Resource> listResourcesWithType(ApiResource apiResource, String namespace, CommandLine.Model.CommandSpec commandSpec) {
        return apiResource.isNamespaced() ? namespacedClient.list(namespace, apiResource.getPath(), loginService.getAuthorization())
                : nonNamespacedClient.list(loginService.getAuthorization(), apiResource.getPath());
    }

    /**
     * Get a resource by type and name
     * @param apiResource The resource type
     * @param namespace The namespace
     * @param resourceName The resource name
     * @param throwError true if error should be thrown
     * @return A resource
     */
    public Resource getSingleResourceWithType(ApiResource apiResource, String namespace, String resourceName, boolean throwError) {
        HttpResponse<Resource> response = apiResource.isNamespaced() ? namespacedClient.get(namespace, apiResource.getPath(), resourceName, loginService.getAuthorization())
                : nonNamespacedClient.get(loginService.getAuthorization(), apiResource.getPath(), resourceName);

        // Micronaut does not throw exception on 404, so produce a 404 manually
        if (response.getStatus().equals(HttpStatus.NOT_FOUND) && throwError) {
            throw new HttpClientResponseException(response.reason(), response);
        }

        return response.body();
    }

    /**
     * Apply a given resource
     * @param apiResource The resource type
     * @param namespace The namespace
     * @param resource The resource
     * @param dryRun Is dry run mode ?
     * @param commandSpec The command that triggered the action
     * @return An HTTP response
     */
    public HttpResponse<Resource> apply(ApiResource apiResource, String namespace, Resource resource, boolean dryRun, CommandLine.Model.CommandSpec commandSpec) {
        try {
            HttpResponse<Resource> response = apiResource.isNamespaced() ?
                    namespacedClient.apply(namespace, apiResource.getPath(), loginService.getAuthorization(), resource, dryRun)
                    : nonNamespacedClient.apply(loginService.getAuthorization(), apiResource.getPath(), resource, dryRun);

            commandSpec.commandLine().getOut().println(formatService.prettifyKind(response.body().getKind())
                    + " \"" + response.body().getMetadata().getName() + "\""
                    + (response.header("X-Ns4kafka-Result") != null ? " " + response.header("X-Ns4kafka-Result") : "") + ".");
            return response;
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, resource.getKind(), resource.getMetadata().getName(), commandSpec);
            return null;
        }
    }

    /**
     * Delete a given resource
     * @param apiResource The resource type
     * @param namespace The namespace
     * @param resource The resource
     * @param dryRun Is dry run mode ?
     * @param commandSpec The command that triggered the action
     * @return true if deletion succeeded, false otherwise
     */
    public boolean delete(ApiResource apiResource, String namespace, String resource, boolean dryRun, CommandLine.Model.CommandSpec commandSpec) {
        try {
            HttpResponse<Void> response = apiResource.isNamespaced() ?
                    namespacedClient.delete(namespace, apiResource.getPath(), resource, loginService.getAuthorization(), dryRun)
                    : nonNamespacedClient.delete(loginService.getAuthorization(), apiResource.getPath(), resource, dryRun);

            // Micronaut does not throw exception on 404, so produce a 404 manually
            if (response.getStatus().equals(HttpStatus.NOT_FOUND)) {
                throw new HttpClientResponseException(response.reason(), response);
            }

            return true;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, apiResource.getKind(), resource, commandSpec);
            return false;
        }
    }

    /**
     * Import all resources of given types
     * @param apiResources The resource types
     * @param namespace The namespace
     * @param dryRun Is dry run mode ?
     * @param commandSpec The command that triggered the action
     * @return 0 if the command succeed, 1 otherwise
     */
    public int importAll(List<ApiResource> apiResources, String namespace, boolean dryRun, CommandLine.Model.CommandSpec commandSpec) {
        int errors = apiResources
                .stream()
                .map(apiResource -> {
                    try {
                        List<Resource> resources = namespacedClient.importResources(namespace, apiResource.getPath(), loginService.getAuthorization(), dryRun);
                        if (!resources.isEmpty()) {
                            formatService.displayList(apiResource.getKind(), resources, TABLE, commandSpec);
                        } else {
                            commandSpec.commandLine().getOut().println("No " + apiResource.getKind().toLowerCase() + " to import.");
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
     * Delete records for a given topic
     * @param namespace The namespace
     * @param topic     The topic to delete records
     * @param dryRun    Is dry run mode or not ?
     * @param commandSpec The command that triggered the action
     * @return The deleted records response
     */
    public int deleteRecords(String namespace, String topic, boolean dryRun, CommandLine.Model.CommandSpec commandSpec) {
        try {
            List<Resource> resources = namespacedClient.deleteRecords(loginService.getAuthorization(), namespace, topic, dryRun);
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
     * Reset offsets for a given topic and consumer group
     * @param namespace The namespace
     * @param group     The consumer group
     * @param resource  The information about how to reset
     * @param dryRun    Is dry run mode or not ?
     * @param commandSpec The command that triggered the action
     * @return 0 if the command succeeded, 1 otherwise
     */
    public int resetOffsets(String namespace, String group, Resource resource, boolean dryRun, CommandLine.Model.CommandSpec commandSpec) {
        try {
            List<Resource> resources = namespacedClient.resetOffsets(loginService.getAuthorization(), namespace, group, resource, dryRun);
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
     * Change the state of a given connector
     * @param namespace The namespace
     * @param connector The connector name
     * @param changeConnectorState The state
     * @param commandSpec The command that triggered the action
     * @return The resource
     */
    public Optional<Resource> changeConnectorState(String namespace, String connector, Resource changeConnectorState, CommandLine.Model.CommandSpec commandSpec) {
        try {
            HttpResponse<Resource> response = namespacedClient.changeConnectorState(namespace, connector, changeConnectorState, loginService.getAuthorization());

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
     * Change the compatibility of a given schema
     * @param namespace The namespace
     * @param subject The schema subject
     * @param compatibility The compatibility to apply
     * @param commandSpec The command that triggered the action
     * @return The resource
     */
    public Optional<Resource> changeSchemaCompatibility(String namespace, String subject, SchemaCompatibility compatibility, CommandLine.Model.CommandSpec commandSpec) {
        try {
            HttpResponse<Resource> response = namespacedClient.changeSchemaCompatibility(namespace, subject,
                    Map.of("compatibility", compatibility.name()), loginService.getAuthorization());

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
     * Reset user password
     * @param namespace The namespace
     * @param user The user
     * @param output The output format
     * @param commandSpec The command that triggered the action
     * @return 0 if the command succeeded, 1 otherwise
     */
    public int resetPassword(String namespace, String user, String output, CommandLine.Model.CommandSpec commandSpec) {
        try {
            HttpResponse<Resource> response = namespacedClient.resetPassword(namespace, user, loginService.getAuthorization());

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
     * List all available connect clusters for vaulting
     * @param namespace The namespace
     * @param commandSpec The command that triggered the action
     * @return 0 if the command succeeded, 1 otherwise
     */
    public int listAvailableVaultsConnectClusters(String namespace, CommandLine.Model.CommandSpec commandSpec) {
        try {
            List<Resource> availableConnectClusters = namespacedClient.listAvailableVaultsConnectClusters(namespace, loginService.getAuthorization());
            formatService.displayList(CONNECT_CLUSTER, availableConnectClusters, TABLE, commandSpec);
            return 0;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }

    /**
     * Vault a list of passwords for a specific Kafka Connect Cluster.
     * @param namespace      The namespace
     * @param connectCluster The Kafka connect cluster to use for vaulting secret.
     * @param passwords      The list of passwords to encrypt.
     * @param commandSpec The command that triggered the action
     * @return 0 if the command succeeded, 1 otherwise
     */
    public int vaultsOnConnectClusters(final String namespace, final String connectCluster, final List<String> passwords, CommandLine.Model.CommandSpec commandSpec) {
        try {
            List<Resource> results = namespacedClient.vaultsOnConnectClusters(namespace, connectCluster, passwords, loginService.getAuthorization());
            formatService.displayList(VAULT_RESPONSE, results, TABLE, commandSpec);
            return 0;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }

    /**
     * Parse resources in given directory/file
     * @param file The directory/file to parse
     * @param recursive Explore given directory recursively or not ?
     * @param commandSpec The command that triggered the action
     * @return The list of resources
     */
    public List<Resource> parseResources(Optional<File> file, boolean recursive, CommandLine.Model.CommandSpec commandSpec) {
        if (file.isPresent()) {
            // List all files to process
            List<File> yamlFiles = fileService.computeYamlFileList(file.get(), recursive);
            if (yamlFiles.isEmpty()) {
                throw new CommandLine.ParameterException(commandSpec.commandLine(), "Could not find YAML or YML files in " + file.get().getName() + " directory.");
            }
            // Load each files
            return fileService.parseResourceListFromFiles(yamlFiles);
        }

        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\\Z");
        return fileService.parseResourceListFromString(scanner.next());
    }
}
