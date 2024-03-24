package com.michelin.kafkactl.command;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.hook.DryRunHook;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.service.ResourceService;
import com.michelin.kafkactl.util.VersionProvider;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;
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

        String namespace = Kafkactl.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

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
                .toList();
        }

        // Otherwise, check resource exists
        Optional<ApiResource> optionalApiResource =
            apiResourcesService.getResourceDefinitionByName(resourceType);
        if (optionalApiResource.isEmpty()) {
            throw new ParameterException(commandSpec.commandLine(),
                "The server does not have resource type " + resourceType + ".");
        }

        if (!optionalApiResource.get().isSynchronizable()) {
            throw new ParameterException(commandSpec.commandLine(),
                "Resource of type " + resourceType + " is not synchronizable.");
        }

        return List.of(optionalApiResource.get());
    }
}
