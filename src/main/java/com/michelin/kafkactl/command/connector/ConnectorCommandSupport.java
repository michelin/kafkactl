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

import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECTOR;

import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

/** Shared support for connector commands. */
public final class ConnectorCommandSupport {
    private ConnectorCommandSupport() {}

    /**
     * Resolve the supplied connector names.
     *
     * @param connectors The requested connector names
     * @param namespace The namespace
     * @param apiResourcesService The API resources service
     * @param resourceService The resource service
     * @param commandSpec The command that triggered the action
     * @return The resolved connector names
     */
    public static List<String> resolveConnectors(
            List<String> connectors,
            String namespace,
            ApiResourcesService apiResourcesService,
            ResourceService resourceService,
            CommandSpec commandSpec) {
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

    /**
     * Execute an offset operation for the supplied connectors.
     *
     * @param connectors The requested connector names
     * @param namespace The namespace
     * @param responseKind The operation response kind
     * @param apiResourcesService The API resources service
     * @param resourceService The resource service
     * @param formatService The format service
     * @param commandSpec The command that triggered the action
     * @param connectorProcessor The operation to execute for each connector
     * @return The command return code
     */
    public static int executeOffsetOperation(
            List<String> connectors,
            String namespace,
            String responseKind,
            ApiResourcesService apiResourcesService,
            ResourceService resourceService,
            FormatService formatService,
            CommandSpec commandSpec,
            Function<String, Stream<Resource>> connectorProcessor) {
        try {
            List<Resource> responses =
                    resolveConnectors(connectors, namespace, apiResourcesService, resourceService, commandSpec).stream()
                            .flatMap(connectorProcessor)
                            .toList();

            if (responses.isEmpty()) {
                return 1;
            }

            formatService.displayList(responseKind, responses, TABLE, commandSpec);
            return 0;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }
}
