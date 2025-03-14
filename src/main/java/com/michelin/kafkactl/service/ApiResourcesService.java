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

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Resource;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;

/** Api resources service. */
@Singleton
public class ApiResourcesService {
    @Inject
    @ReflectiveAccess
    private ClusterResourceClient resourceClient;

    @Inject
    @ReflectiveAccess
    private LoginService loginService;

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
        return listResourceDefinitions().stream()
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
        return listResourceDefinitions().stream()
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
        List<String> allowedKinds =
                listResourceDefinitions().stream().map(ApiResource::getKind).toList();

        return resources.stream()
                .filter(resource -> !allowedKinds.contains(resource.getKind()))
                .toList();
    }
}
