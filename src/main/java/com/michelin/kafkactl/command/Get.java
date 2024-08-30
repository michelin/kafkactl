package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.service.FormatService.YAML;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/**
 * Get subcommand.
 */
@Command(name = "get",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Get resources by resource type for the current namespace.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true)
public class Get extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

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
        String namespace = getNamespace();

        // List resources based on parameters
        if (resourceName.isEmpty() || apiResources.size() > 1) {
            return resourceService.listAll(apiResources, namespace, output, commandSpec);
        }

        try {
            // Get individual resources for given types (k get topic topic1)
            Resource singleResource =
                resourceService.getSingleResourceWithType(apiResources.getFirst(), namespace, resourceName.get(), true);
            formatService.displaySingle(singleResource, output, commandSpec);
            return 0;
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, apiResources.getFirst().getKind(), resourceName.get(), commandSpec);
            return 1;
        }
    }

    /**
     * Validate required output format.
     */
    private void validateOutput() {
        if (!List.of(TABLE, YAML).contains(output)) {
            throw new ParameterException(commandSpec.commandLine(),
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

        throw new ParameterException(commandSpec.commandLine(),
            "The server does not have resource type " + resourceType + ".");
    }
}
