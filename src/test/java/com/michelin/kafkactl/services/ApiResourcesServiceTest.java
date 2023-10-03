package com.michelin.kafkactl.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiResourcesServiceTest {
    @Mock
    public ClusterResourceClient resourceClient;

    @Mock
    public LoginService loginService;

    @InjectMocks
    public ApiResourcesService apiResourcesService;

    @Test
    void shouldListResourceDefinitions() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(resourceClient.listResourceDefinitions(any()))
            .thenReturn(Collections.singletonList(apiResource));

        List<ApiResource> actual = apiResourcesService.listResourceDefinitions();

        assertEquals(Collections.singletonList(apiResource), actual);
    }

    @Test
    void shouldGetResourceDefinitionByKindFound() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        ApiResource otherApiResource = ApiResource.builder()
            .kind("OtherKind")
            .path("others")
            .names(List.of("others", "other", "ot"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(resourceClient.listResourceDefinitions(any()))
            .thenReturn(List.of(apiResource, otherApiResource));

        Optional<ApiResource> actual = apiResourcesService.getResourceDefinitionByKind("Topic");

        assertTrue(actual.isPresent());
        assertEquals(apiResource, actual.get());
    }

    @Test
    void shouldGetResourceDefinitionByKindNotFound() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        ApiResource otherApiResource = ApiResource.builder()
            .kind("OtherKind")
            .path("others")
            .names(List.of("others", "other", "ot"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(resourceClient.listResourceDefinitions(any()))
            .thenReturn(List.of(apiResource, otherApiResource));

        Optional<ApiResource> actual = apiResourcesService.getResourceDefinitionByKind("notFound");

        assertFalse(actual.isPresent());
    }

    @Test
    void shouldGetResourceDefinitionByCommandNameFound() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        ApiResource otherApiResource = ApiResource.builder()
            .kind("OtherKind")
            .path("others")
            .names(List.of("others", "other", "ot"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(resourceClient.listResourceDefinitions(any()))
            .thenReturn(List.of(apiResource, otherApiResource));

        Optional<ApiResource> actual = apiResourcesService.getResourceDefinitionByName("topics");

        assertTrue(actual.isPresent());
        assertEquals(apiResource, actual.get());
    }

    @Test
    void shouldGetResourceDefinitionByCommandNameNotFound() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        ApiResource otherApiResource = ApiResource.builder()
            .kind("OtherKind")
            .path("others")
            .names(List.of("others", "other", "ot"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(resourceClient.listResourceDefinitions(any()))
            .thenReturn(List.of(apiResource, otherApiResource));

        Optional<ApiResource> actual = apiResourcesService.getResourceDefinitionByName("notFound");

        assertFalse(actual.isPresent());
    }

    @Test
    void shouldValidateResourceTypesValid() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        ApiResource otherApiResource = ApiResource.builder()
            .kind("OtherKind")
            .path("others")
            .names(List.of("others", "other", "ot"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(ObjectMeta.builder()
                .name("prefix.topic")
                .namespace("namespace")
                .build())
            .spec(Collections.emptyMap())
            .build();

        when(resourceClient.listResourceDefinitions(any()))
            .thenReturn(List.of(apiResource, otherApiResource));

        List<Resource> actual = apiResourcesService.filterNotAllowedResourceTypes(Collections.singletonList(resource));

        assertTrue(actual.isEmpty());
    }

    @Test
    void shouldValidateResourceTypesInvalid() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        ApiResource otherApiResource = ApiResource.builder()
            .kind("OtherKind")
            .path("others")
            .names(List.of("others", "other", "ot"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        Resource resource = Resource.builder()
            .kind("Invalid")
            .build();

        when(resourceClient.listResourceDefinitions(any()))
            .thenReturn(List.of(apiResource, otherApiResource));

        List<Resource> actual = apiResourcesService.filterNotAllowedResourceTypes(Collections.singletonList(resource));

        assertEquals(resource, actual.get(0));
    }
}
