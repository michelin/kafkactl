package com.michelin.kafkactl.services;

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class ApiResourcesService {
    @Inject
    public ClusterResourceClient resourceClient;

    @Inject
    public LoginService loginService;

    public List<ApiResource> getListResourceDefinition() {
        return resourceClient.listResourceDefinitions(loginService.getAuthorization());
    }

    public Optional<ApiResource> getResourceDefinitionFromKind(String kind) {
        List<ApiResource> apiResources = getListResourceDefinition();
        return apiResources.stream()
                .filter(resource -> resource.getKind().equals(kind))
                .findFirst();
    }

    public Optional<ApiResource> getResourceDefinitionFromCommandName(String name) {
        List<ApiResource> apiResources = getListResourceDefinition();
        return apiResources.stream()
                .filter(resource -> resource.getNames().contains(name))
                .findFirst();
    }

    public List<Resource> validateResourceTypes(List<Resource> resources) {
        List<String> allowedKinds = this.getListResourceDefinition()
                .stream()
                .map(ApiResource::getKind)
                .collect(Collectors.toList());

        return resources.stream()
                .filter(resource -> !allowedKinds.contains(resource.getKind()))
                .collect(Collectors.toList());
    }
}
