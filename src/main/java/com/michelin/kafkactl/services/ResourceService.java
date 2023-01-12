package com.michelin.kafkactl.services;

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.NamespacedResourceClient;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.models.SchemaCompatibility;
import com.michelin.kafkactl.models.Status;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public Map<ApiResource, List<Resource>> listAll(List<ApiResource> apiResources, String namespace) {
        return apiResources
                .stream()
                .map(apiResource -> Map.entry(apiResource, listResourcesWithType(apiResource, namespace)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<Resource> listResourcesWithType(ApiResource apiResource, String namespace) {
        try {
            if (apiResource.isNamespaced()) {
                return namespacedClient.list(namespace, apiResource.getPath(), loginService.getAuthorization());
            } else {
                return nonNamespacedClient.list(loginService.getAuthorization(), apiResource.getPath());
            }
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, apiResource.getKind(), null);
        }
        return List.of();
    }

    public Resource getSingleResourceWithType(ApiResource apiResource, String namespace, String resourceName, boolean throwError) {
        Resource resource;
        if (apiResource.isNamespaced()) {
            resource = namespacedClient.get(namespace, apiResource.getPath(), resourceName, loginService.getAuthorization());
        } else {
            resource = nonNamespacedClient.get(loginService.getAuthorization(), apiResource.getPath(), resourceName);
        }
        if (resource == null && throwError) {
            // micronaut converts HTTP 404 into null
            // produce a 404
            Status notFoundStatus = Status.builder()
                    .code(404)
                    .message("Resource not found")
                    .reason("NotFound")
                    .build();
            throw new HttpClientResponseException("Not Found", HttpResponse.notFound(notFoundStatus));
        }
        return resource;
    }

    public HttpResponse<Resource> apply(ApiResource apiResource, String namespace, Resource resource, boolean dryRun) {
        try {
            if (apiResource.isNamespaced()) {
                return namespacedClient.apply(namespace, apiResource.getPath(), loginService.getAuthorization(), resource, dryRun);
            } else {
                return nonNamespacedClient.apply(loginService.getAuthorization(), apiResource.getPath(), resource, dryRun);
            }
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, apiResource.getKind(), resource.getMetadata().getName());
        }

        return null;
    }

    public boolean delete(ApiResource apiResource, String namespace, String resource, boolean dryRun) {
        try {
            if (apiResource.isNamespaced()) {
                HttpResponse<Void> response = namespacedClient.delete(namespace, apiResource.getPath(), resource, loginService.getAuthorization(), dryRun);
                if(response.getStatus() != HttpStatus.NO_CONTENT){
                    throw new HttpClientResponseException("Resource not Found", response);
                }
                return true;
            } else {
                nonNamespacedClient.delete(loginService.getAuthorization(), apiResource.getPath(), resource, dryRun);
                return true;
            }
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, apiResource.getKind(), resource);
        }
        return false;
    }

    public Map<ApiResource, List<Resource>> importAll(List<ApiResource> apiResources, String namespace, boolean dryRun) {
        return apiResources
                .stream()
                .map(apiResource -> Map.entry(apiResource, importResourcesWithType(apiResource, namespace, dryRun)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<Resource> importResourcesWithType(ApiResource apiResource, String namespace, boolean dryRun) {
        List<Resource> resources;

        try {
            resources = namespacedClient.importResources(namespace, apiResource.getPath(), loginService.getAuthorization(), dryRun);
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, apiResource.getKind(), null);
            resources = List.of();
        }

        return resources;
    }

    /**
     * Delete records for a given topic
     * @param namespace The namespace
     * @param topic The topic to delete records
     * @param dryrun Is dry run mode or not ?
     * @return The delete records response
     */
    public List<Resource> deleteRecords(String namespace, String topic, boolean dryrun) {
        List<Resource> resources = List.of();

        try {
            return namespacedClient.deleteRecords(loginService.getAuthorization(), namespace, topic, dryrun);
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, "Topic", topic);
        }

        return resources;
    }

    /**
     * Reset offsets for a given topic and consumer group
     * @param namespace The namespace
     * @param group The consumer group
     * @param resource The information about how to reset
     * @param dryRun Is dry run mode or not ?
     * @return The reset offsets response
     */
    public List<Resource> resetOffsets(String namespace, String group, Resource resource, boolean dryRun) {
        List<Resource> resources = List.of();

        try {
            resources = namespacedClient.resetOffsets(loginService.getAuthorization(), namespace, group, resource, dryRun);
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, "ConsumerGroup", group);
        }

        return resources;
    }

    public Resource changeConnectorState(String namespace, String connector, Resource changeConnectorState) {
        try {
            Resource resource = namespacedClient.changeConnectorState(namespace, connector, changeConnectorState, loginService.getAuthorization());
            if (resource == null) {
                // micronaut converts HTTP 404 into null
                // produce a 404
                Status notFoundStatus = Status.builder()
                        .code(404)
                        .message("Resource not found")
                        .reason("NotFound")
                        .build();
                throw new HttpClientResponseException("Not Found", HttpResponse.notFound(notFoundStatus));
            }
            return resource;
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, "ChangeConnectorState", connector);
        }
        return null;
    }

    public Resource changeSchemaCompatibility(String namespace, String subject, SchemaCompatibility compatibility) {
        try {
            Resource resource = namespacedClient.changeSchemaCompatibility(namespace, subject,
                    Map.of("compatibility", compatibility), loginService.getAuthorization());

            if (resource == null) {
                // micronaut converts HTTP 404 into null
                // produce a 404
                Status notFoundStatus = Status.builder()
                        .code(404)
                        .message("Resource not found")
                        .reason("NotFound")
                        .build();
                throw new HttpClientResponseException("Not Found", HttpResponse.notFound(notFoundStatus));
            }
            return resource;
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, "Schema", subject);
        }
        return null;
    }

    public Resource resetPassword(String namespace, String user) {
        try {
            Resource resource = namespacedClient.resetPassword(namespace, user, loginService.getAuthorization());

            if (resource == null) {
                // micronaut converts HTTP 404 into null
                // produce a 404
                Status notFoundStatus = Status.builder()
                        .code(404)
                        .message("Resource not found")
                        .reason("NotFound")
                        .build();
                throw new HttpClientResponseException("Not Found", HttpResponse.notFound(notFoundStatus));
            }
            return resource;
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, "KafkaUserResetPassword", namespace);
        }
        return null;
    }
}
