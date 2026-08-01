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
import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECTOR_OFFSET_RESPONSE;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/** List connector offsets subcommand. */
@Command(
        name = "list-offsets",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "List connector offsets.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class ConnectorListOffsets extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Parameters(
            index = "0..*",
            description = "Connector names separated by space or \"all\" for all connectors.",
            arity = "1..*")
    public List<String> connectors;

    /**
     * Run the "connector list-offsets" command.
     *
     * @return The command return code
     */
    @Override
    public Integer onAuthSuccess() {
        String namespace = getNamespace();
        boolean allConnectors = connectors.stream().anyMatch(connector -> connector.equalsIgnoreCase("ALL"));

        try {
            if (allConnectors) {
                ApiResource connectorType = apiResourcesService
                        .getResourceDefinitionByKind(CONNECTOR)
                        .orElseThrow(() -> new ParameterException(
                                commandSpec.commandLine(), "The server does not have resource type Connector."));

                connectors = resourceService.listResourcesWithType(connectorType, namespace, "*", null).stream()
                        .map(resource -> resource.getMetadata().getName())
                        .toList();
            }

            List<Resource> offsets = connectors.stream()
                    .flatMap(connector ->
                            resourceService.listConnectorOffsets(namespace, connector, commandSpec).stream())
                    .toList();

            if (!offsets.isEmpty()) {
                formatService.displayList(CONNECTOR_OFFSET_RESPONSE, offsets, TABLE, commandSpec);
                return 0;
            }

            return 1;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }
}
