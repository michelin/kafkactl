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

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.michelin.kafkactl.hook.AuthenticatedHook;
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
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/** Diff subcommand. */
@Command(
        name = "diff",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Get differences between a new resource and a old resource.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class Diff extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Option(
            names = {"-f", "--file"},
            description = "YAML file or directory containing resources to compare.")
    public Optional<File> file;

    @Option(
            names = {"-R", "--recursive"},
            description = "Search file recursively.")
    public boolean recursive;

    @Option(
            names = {"--ignore-fields"},
            description = "Comma-separated list of YAML paths to ignore (e.g., metadata.labels.creationDateTime)",
            split = ",")
    public List<String> ignoreFields = List.of();

    /**
     * Run the "diff" command.
     *
     * @return The command return code
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
            super.validateNamespace(resources);
            List<Resource> preparedResources = resourceService.prepareResources(resources, commandSpec);
            return diffResources(preparedResources, getNamespace());
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, commandSpec);
            return 1;
        }
    }

    private int diffResources(List<Resource> resources, String namespace) {
        int errorCount = resources.stream()
                .mapToInt(resource -> diffResource(namespace, resource))
                .sum();
        return errorCount > 0 ? 1 : 0;
    }

    private int diffResource(String namespace, Resource resource) {
        ApiResource apiResource = apiResourcesService
                .getResourceDefinitionByKind(resource.getKind())
                .orElseThrow();
        Resource live = resourceService.getSingleResourceWithType(
                apiResource, namespace, resource.getMetadata().getName(), false);
        HttpResponse<Resource> merged = resourceService.apply(apiResource, namespace, resource, true, commandSpec);
        if (merged != null && merged.getBody().isPresent()) {
            List<String> unifiedDiff = unifiedDiff(live, merged.body());
            unifiedDiff.forEach(diff -> commandSpec.commandLine().getOut().println(diff));
            return 0;
        }
        return 1;
    }

    /**
     * Compute the difference between current resource and applied resource.
     *
     * @param live The current resource
     * @param merged The applied new resource
     * @return The differences
     */
    private List<String> unifiedDiff(Resource live, Resource merged) {
        // Ignore status and timestamp for comparison
        if (live != null) {
            live.setStatus(null);
            live.getMetadata().setCreationTimestamp(null);
        }
        merged.setStatus(null);
        merged.getMetadata().setCreationTimestamp(null);

        DumperOptions options = new DumperOptions();
        options.setExplicitStart(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer(new DumperOptions());
        representer.addClassTag(Resource.class, Tag.MAP);
        Yaml yaml = new Yaml(representer, options);

        String liveYaml = live != null ? yaml.dump(live) : "";
        String mergedYaml = yaml.dump(merged);

        // Remove ignored fields
        if (!ignoreFields.isEmpty()) {
            liveYaml = removeIgnoredFields(liveYaml, ignoreFields, yaml);
            mergedYaml = removeIgnoredFields(mergedYaml, ignoreFields, yaml);
        }

        List<String> oldResourceStr =
                liveYaml.isEmpty() ? List.of() : liveYaml.lines().toList();
        List<String> newResourceStr = mergedYaml.lines().toList();
        Patch<String> diff = DiffUtils.diff(oldResourceStr, newResourceStr);
        return UnifiedDiffUtils.generateUnifiedDiff(
                String.format(
                        "%s/%s-LIVE", merged.getKind(), merged.getMetadata().getName()),
                String.format(
                        "%s/%s-MERGED", merged.getKind(), merged.getMetadata().getName()),
                oldResourceStr,
                diff,
                3);
    }

    /**
     * Remove ignored fields from YAML content.
     *
     * @param yamlContent The YAML content
     * @param ignoreFields List of dot-notation paths to ignore
     * @param yaml The YAML instance for parsing/dumping
     * @return The YAML content without ignored fields
     */
    private String removeIgnoredFields(String yamlContent, List<String> ignoreFields, Yaml yaml) {
        if (yamlContent.isEmpty()) {
            return yamlContent;
        }

        Object data = yaml.load(yamlContent);
        if (!(data instanceof Map)) {
            return yamlContent;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) data;

        // Remove each ignored field using removeFieldRecursive directly
        for (String path : ignoreFields) {
            List<String> parts = List.of(path.split("\\."));
            removeField(map, parts);
        }

        // Dump back to YAML
        return yaml.dump(map);
    }

    /**
     * Remove a field from a map using a list of path parts. Handles both nested and flat keys.
     *
     * @param map The map to modify
     * @param parts The list of path parts
     */
    @SuppressWarnings("unchecked")
    private void removeField(Map<String, Object> map, List<String> parts) {
        if (parts.isEmpty()) {
            return;
        }
        // Remove flat key if present
        String flatKey = String.join(".", parts);
        map.remove(flatKey);

        // Recurse into nested map if possible
        Object next = map.get(parts.getFirst());
        if (next instanceof Map) {
            removeField((Map<String, Object>) next, parts.subList(1, parts.size()));
        }
    }
}
