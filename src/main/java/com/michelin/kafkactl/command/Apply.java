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
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/** Apply subcommand. */
@Command(
        name = "apply",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Create or update a resource.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class Apply extends DryRunHook {
    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Option(
            names = {"-f", "--file"},
            description = "YAML file or directory containing resources to apply.")
    public Optional<File> file;

    @Option(
            names = {"-R", "--recursive"},
            description = "Search file recursively.")
    public boolean recursive;

    /**
     * Run the "apply" command.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws IOException {
        // If we have none or both stdin and File set, we stop
        boolean hasStdin = System.in.available() > 0;
        if (hasStdin == file.isPresent()) {
            throw new ParameterException(commandSpec.commandLine(), "Required one of -f or stdin.");
        }

        if (file.isPresent() && !file.get().exists()) {
            throw new ParameterException(
                    commandSpec.commandLine(),
                    "File or directory not found: " + file.get().getAbsolutePath());
        }

        List<Resource> resources = resourceService.parseResources(file, recursive, commandSpec);
        try {
            resourceService.validateAllowedResources(resources, commandSpec);
            validateNamespace(resources);
            List<Resource> preparedResources = resourceService.prepareResources(resources, commandSpec);
            return applyResources(preparedResources, getNamespace());
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, commandSpec);
            return 1;
        }
    }

    private int applyResources(List<Resource> resources, String namespace) {
        int errorCount = resources.stream()
                .mapToInt(resource -> applyResource(namespace, resource))
                .sum();
        return errorCount > 0 ? 1 : 0;
    }

    private int applyResource(String namespace, Resource resource) {
        ApiResource apiResource = apiResourcesService
                .getResourceDefinitionByKind(resource.getKind())
                .orElseThrow();
        HttpResponse<Resource> httpRes = resourceService.apply(apiResource, namespace, resource, dryRun, commandSpec);
        return (httpRes != null ? 0 : 1);
    }
}
