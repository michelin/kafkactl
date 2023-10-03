package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.parents.DryRunCommand;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Import subcommand.
 */
@Command(name = "import",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Import non-synchronized resources.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ImportSubcommand extends DryRunCommand {
    @Inject
    public ResourceService resourceService;

    @Inject
    public FormatService formatService;

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

        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        return resourceService.importAll(apiResources, namespace, dryRun, commandSpec);
    }

    /**
     * Validate required resource type.
     *
     * @return The list of resource type
     */
    private List<ApiResource> validateResourceType() {
        // Specific case ALL
        if (resourceType.equalsIgnoreCase("ALL")) {
            return apiResourcesService.listResourceDefinitions()
                .stream()
                .filter(ApiResource::isSynchronizable)
                .collect(Collectors.toList());
        }

        // Otherwise, check resource exists
        Optional<ApiResource> optionalApiResource =
            apiResourcesService.getResourceDefinitionByCommandName(resourceType);
        if (optionalApiResource.isEmpty()) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                "The server does not have resource type " + resourceType + ".");
        }

        if (!optionalApiResource.get().isSynchronizable()) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                "Resource of type " + resourceType + " is not synchronizable.");
        }

        return List.of(optionalApiResource.get());
    }
}
