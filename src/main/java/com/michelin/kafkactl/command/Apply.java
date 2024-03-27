package com.michelin.kafkactl.command;

import com.michelin.kafkactl.hook.DryRunHook;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import com.michelin.kafkactl.util.VersionProvider;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/**
 * Apply subcommand.
 */
@Command(name = "apply",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Create or update a resource.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class Apply extends DryRunHook {
    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Option(names = {"-f", "--file"}, description = "YAML file or directory containing resources.")
    public Optional<File> file;

    @Option(names = {"-R", "--recursive"}, description = "Search file recursively.")
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
