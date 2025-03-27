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
package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.RESOURCE_DEFINITION;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import picocli.CommandLine.Command;

/** Api resources subcommand. */
@Command(
        name = "api-resources",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Print the supported API resources on the server.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class ApiResources extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    /**
     * Run the "api-resources" command.
     *
     * @return The command return code
     */
    @Override
    public Integer onAuthSuccess() {
        try {
            List<Resource> resources = apiResourcesService.listResourceDefinitions().stream()
                    .map(apiResource -> Resource.builder()
                            .metadata(Metadata.builder()
                                    .name(apiResource.getKind())
                                    .build())
                            .spec(Map.of(
                                    "names",
                                    String.join(",", apiResource.getNames()),
                                    "namespaced",
                                    String.valueOf(apiResource.isNamespaced()),
                                    "synchronizable",
                                    String.valueOf(apiResource.isSynchronizable())))
                            .build())
                    .toList();

            formatService.displayList(RESOURCE_DEFINITION, resources, TABLE, commandSpec);
            return 0;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }
}
