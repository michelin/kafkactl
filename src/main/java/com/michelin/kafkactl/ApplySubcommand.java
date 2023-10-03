package com.michelin.kafkactl;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.parents.DryRunCommand;
import com.michelin.kafkactl.services.FileService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Apply subcommand.
 */
@CommandLine.Command(name = "apply",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Create or update a resource.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ApplySubcommand extends DryRunCommand {
    @Inject
    public FormatService formatService;

    @Inject
    public FileService fileService;

    @Inject
    public ResourceService resourceService;

    @Option(names = {"-f", "--file"}, description = "YAML file or directory containing resources.")
    public Optional<File> file;

    @Option(names = {"-R", "--recursive"}, description = "Search file recursively.")
    public boolean recursive;

    /**
     * Run the "apply" command.
     *
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws Exception {
        // If we have none or both stdin and File set, we stop
        boolean hasStdin = System.in.available() > 0;
        if (hasStdin == file.isPresent()) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Required one of -f or stdin.");
        }

        List<Resource> resources = resourceService.parseResources(file, recursive, commandSpec);

        try {
            resourceService.validateAllowedResources(resources, commandSpec);
            validateNamespace(resources);
            resourceService.enrichSchemaContent(resources, commandSpec);

            int errors = resources.stream()
                .map(resource -> {
                    ApiResource apiResource =
                        apiResourcesService.getResourceDefinitionByKind(resource.getKind()).orElseThrow();
                    return resourceService.apply(apiResource, getNamespace(), resource, dryRun, commandSpec);
                })
                .mapToInt(value -> value != null ? 0 : 1)
                .sum();

            return errors > 0 ? 1 : 0;
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, commandSpec);
            return 1;
        }
    }
}
