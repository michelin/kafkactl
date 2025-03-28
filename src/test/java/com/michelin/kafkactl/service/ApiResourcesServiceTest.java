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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
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
    ClusterResourceClient resourceClient;

    @Mock
    LoginService loginService;

    @InjectMocks
    ApiResourcesService apiResourcesService;

    @Test
    void shouldListResourceDefinitions() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(resourceClient.listResourceDefinitions(any())).thenReturn(Collections.singletonList(apiResource));

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

        when(resourceClient.listResourceDefinitions(any())).thenReturn(List.of(apiResource, otherApiResource));

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

        when(resourceClient.listResourceDefinitions(any())).thenReturn(List.of(apiResource, otherApiResource));

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

        when(resourceClient.listResourceDefinitions(any())).thenReturn(List.of(apiResource, otherApiResource));

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

        when(resourceClient.listResourceDefinitions(any())).thenReturn(List.of(apiResource, otherApiResource));

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
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        when(resourceClient.listResourceDefinitions(any())).thenReturn(List.of(apiResource, otherApiResource));

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

        Resource resource = Resource.builder().kind("Invalid").build();

        when(resourceClient.listResourceDefinitions(any())).thenReturn(List.of(apiResource, otherApiResource));

        List<Resource> actual = apiResourcesService.filterNotAllowedResourceTypes(Collections.singletonList(resource));

        assertEquals(resource, actual.getFirst());
    }
}
