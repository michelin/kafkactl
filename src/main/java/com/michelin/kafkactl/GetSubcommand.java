package com.michelin.kafkactl;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.services.FormatService.YAML;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.parents.AuthenticatedCommand;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Get subcommand.
 */
@Command(name = "get",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Get resources by resource type for the current namespace.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class GetSubcommand extends AuthenticatedCommand {
    @Inject
    public ResourceService resourceService;

    @Inject
    public FormatService formatService;

    @Parameters(index = "0", description = "Resource type or 'all' to display resources of all types.", arity = "1")
    public String resourceType;

    @Parameters(index = "1", description = "Resource name.", arity = "0..1")
    public Optional<String> resourceName;

    @Option(names = {"-o", "--output"}, description = "Output format. One of: yaml|table", defaultValue = "table")
    public String output;

    /**
     * Run the "get" command.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws IOException {
        // Validate resourceType + custom type ALL
        List<ApiResource> apiResources = validateResourceType();

        validateOutput();

        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        // List resources based on parameters
        if (resourceName.isEmpty() || apiResources.size() > 1) {
            return resourceService.listAll(apiResources, namespace, commandSpec);
        }

        try {
            // Get individual resources for given types (k get topic topic1)
            Resource singleResource =
                resourceService.getSingleResourceWithType(apiResources.get(0), namespace, resourceName.get(), true);
            formatService.displaySingle(singleResource, output, commandSpec);
            return 0;
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, apiResources.get(0).getKind(), resourceName.get(), commandSpec);
            return 1;
        }
    }

    /**
     * Validate required output format.
     */
    private void validateOutput() {
        if (!List.of(TABLE, YAML).contains(output)) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                "Invalid value " + output + " for option -o.");
        }
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
                .filter(ApiResource::isNamespaced)
                .toList();
        }

        // Otherwise, check resource exists
        Optional<ApiResource> optionalApiResource =
            apiResourcesService.getResourceDefinitionByName(resourceType);
        if (optionalApiResource.isPresent()) {
            return List.of(optionalApiResource.get());
        }

        throw new CommandLine.ParameterException(commandSpec.commandLine(),
            "The server does not have resource type " + resourceType + ".");
    }
}
