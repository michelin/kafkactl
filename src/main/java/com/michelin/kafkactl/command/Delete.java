package com.michelin.kafkactl.command;

import com.michelin.kafkactl.hook.DryRunHook;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FileService;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/**
 * Delete subcommand.
 */
@Command(name = "delete",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Delete a resource.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true)
public class Delete extends DryRunHook {
    public static final String VERSION = "version";

    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Inject
    @ReflectiveAccess
    private FileService fileService;

    @ArgGroup(multiplicity = "1")
    public EitherOf config;

    /**
     * Run the "delete" command.
     *
     * @return The command return code
     */
    @Override
    public Integer onAuthSuccess() {
        if (config.nameConfig != null && !config.nameConfig.confirmed && !dryRun
            && (config.nameConfig.resourceName.contains("*") || config.nameConfig.resourceName.contains("?"))) {
            commandSpec.commandLine().getOut().println("You are about to potentially delete multiple resources "
                + "with wildcard \"" + config.nameConfig.resourceName + "\".\n"
                + "Rerun the command with option --dry-run to see the resources that will be deleted.\n"
                + "Rerun the command with option --execute to execute this operation.");
            return 0;
        }

        String namespace = getNamespace();
        List<Resource> resources = parseResources(namespace);

        try {
            resourceService.validateAllowedResources(resources, commandSpec);
            validateNamespace(resources);

            // Process each document individually, return 0 when all succeed
            int errors = resources.stream()
                .map(resource -> {
                    ApiResource apiResource =
                        apiResourcesService.getResourceDefinitionByKind(resource.getKind()).orElseThrow();
                    Map<String, Object> spec = resource.getSpec();
                    return resourceService.delete(apiResource, namespace, resource.getMetadata().getName(),
                        (spec != null && spec.containsKey(VERSION) ? spec.get(VERSION).toString() : null),
                        dryRun, commandSpec);
                })
                .mapToInt(value -> Boolean.TRUE.equals(value) ? 0 : 1)
                .sum();

            return errors > 0 ? 1 : 0;
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, commandSpec);
            return 1;
        }
    }

    /**
     * Parse input resources (given by file or by name) to build the list of resources to delete.
     *
     * @param namespace The namespace
     * @return A list of resources
     */
    private List<Resource> parseResources(String namespace) {
        if (config.fileConfig != null && config.fileConfig.file.isPresent()) {
            // List all files to process
            List<File> yamlFiles =
                fileService.computeYamlFileList(config.fileConfig.file.get(), config.fileConfig.recursive);
            if (yamlFiles.isEmpty()) {
                throw new ParameterException(commandSpec.commandLine(),
                    "Could not find YAML or YML files in " + config.fileConfig.file.get().getName() + " directory.");
            }
            // Load each files
            return fileService.parseResourceListFromFiles(yamlFiles);
        }

        Optional<ApiResource> optionalApiResource =
            apiResourcesService.getResourceDefinitionByName(config.nameConfig.resourceType);
        if (optionalApiResource.isEmpty()) {
            throw new ParameterException(commandSpec.commandLine(),
                "The server does not have resource type(s) " + config.nameConfig.resourceType + ".");
        }

        // Generate a single resource with minimum details from input
        var builder = Resource.builder()
            .metadata(Metadata.builder()
                .name(config.nameConfig.resourceName)
                .namespace(namespace)
                .build())
            .kind(optionalApiResource.get().getKind());

        if (config.nameConfig.version.isPresent()) {
            builder = builder.spec(Map.of(VERSION, config.nameConfig.version.get()));
        }
        return List.of(builder.build());
    }

    /**
     * By-name of by-file deletion config.
     */
    public static class EitherOf {
        /**
         * Configuration for deletion by name.
         */
        @ArgGroup(exclusive = false)
        public ByName nameConfig;

        /**
         * Configuration for deletion by file.
         */
        @ArgGroup(exclusive = false)
        public ByFile fileConfig;
    }

    /**
     * By-name deletion config.
     */
    public static class ByName {
        @Parameters(index = "0", description = "Resource type.", arity = "1")
        public String resourceType;

        @Parameters(index = "1", description = "Resource name or wildcard matching resource names.", arity = "1")
        public String resourceName;

        @Option(names = {"-V", "--version"},
            description = "Version to delete. Only with schema resource and name parameter.",
            arity = "0..1")
        public Optional<String> version;

        @Option(names = {"--execute"}, description = "This option is mandatory to delete resources with wildcard")
        public boolean confirmed;
    }

    /**
     * By-file deletion config.
     */
    public static class ByFile {
        @Option(names = {"-f", "--file"}, description = "YAML file or directory containing resources to delete.")
        public Optional<File> file;

        @Option(names = {"-R", "--recursive"}, description = "Search file recursively.")
        public boolean recursive;
    }
}
