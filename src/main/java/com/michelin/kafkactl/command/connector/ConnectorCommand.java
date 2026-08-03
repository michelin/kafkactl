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
package com.michelin.kafkactl.command.connector;

import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECTOR;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.util.List;
import picocli.CommandLine.ParameterException;

/** Base command for connector operations. */
public abstract class ConnectorCommand extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    protected ResourceService resourceService;

    @Inject
    @ReflectiveAccess
    protected FormatService formatService;

    /**
     * Resolve the supplied connector names.
     *
     * @param connectors The requested connector names
     * @param namespace The namespace
     * @return The resolved connector names
     */
    protected List<String> resolveConnectors(List<String> connectors, String namespace) {
        if (connectors.stream().noneMatch(connector -> connector.equalsIgnoreCase("ALL"))) {
            return connectors;
        }

        ApiResource connectorType = apiResourcesService
                .getResourceDefinitionByKind(CONNECTOR)
                .orElseThrow(() -> new ParameterException(
                        commandSpec.commandLine(), "The server does not have resource type Connector."));

        return resourceService.listResourcesWithType(connectorType, namespace, "*", null).stream()
                .map(resource -> resource.getMetadata().getName())
                .toList();
    }
}
