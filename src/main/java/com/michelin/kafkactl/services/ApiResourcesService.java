package com.michelin.kafkactl.services;

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;

/**
 * Api resources service.
 */
@Singleton
public class ApiResourcesService {
    @Inject
    public ClusterResourceClient resourceClient;

    @Inject
    public LoginService loginService;

    /**
     * List all resource definitions.
     *
     * @return A list of API resources
     */
    public List<ApiResource> listResourceDefinitions() {
        return resourceClient.listResourceDefinitions(loginService.getAuthorization());
    }

    /**
     * Get a resource definition by kind.
     *
     * @param kind The kind
     * @return The resource definition if it exists
     */
    public Optional<ApiResource> getResourceDefinitionByKind(String kind) {
        return listResourceDefinitions()
            .stream()
            .filter(resource -> resource.getKind().equals(kind))
            .findFirst();
    }

    /**
     * Get a resource definition by name.
     *
     * @param name The name
     * @return The resource definition if it exists
     */
    public Optional<ApiResource> getResourceDefinitionByName(String name) {
        return listResourceDefinitions()
            .stream()
            .filter(resource -> resource.getNames().contains(name))
            .findFirst();
    }

    /**
     * Get not allowed resources.
     *
     * @param resources The resources
     * @return The allowed resources
     */
    public List<Resource> filterNotAllowedResourceTypes(List<Resource> resources) {
        List<String> allowedKinds = listResourceDefinitions()
            .stream()
            .map(ApiResource::getKind)
            .toList();

        return resources
            .stream()
            .filter(resource -> !allowedKinds.contains(resource.getKind()))
            .toList();
    }
}
