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

import com.michelin.kafkactl.hook.DryRunHook;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/** Import subcommand. */
@Command(
        name = "import",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Import non-synchronized resources.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class Import extends DryRunHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Parameters(index = "0", description = "Resource type.", arity = "1")
    public String resourceType;

    /**
     * Run the "get" command.
     *
     * @return The command return code
     */
    public Integer onAuthSuccess() {
        // Validate resourceType + custom type ALL
        List<ApiResource> apiResources = validateResourceType();
        return resourceService.importAll(apiResources, getNamespace(), dryRun, commandSpec);
    }

    /**
     * Validate required resource type.
     *
     * @return The list of resource type
     */
    private List<ApiResource> validateResourceType() {
        // Specific case ALL
        if (resourceType.equalsIgnoreCase("ALL")) {
            return apiResourcesService.listResourceDefinitions().stream()
                    .filter(ApiResource::isSynchronizable)
                    .toList();
        }

        // Otherwise, check resource exists
        Optional<ApiResource> optionalApiResource = apiResourcesService.getResourceDefinitionByName(resourceType);
        if (optionalApiResource.isEmpty()) {
            throw new ParameterException(
                    commandSpec.commandLine(), "The server does not have resource type " + resourceType + ".");
        }

        if (!optionalApiResource.get().isSynchronizable()) {
            throw new ParameterException(
                    commandSpec.commandLine(), "Resource of type " + resourceType + " is not synchronizable.");
        }

        return List.of(optionalApiResource.get());
    }
}
