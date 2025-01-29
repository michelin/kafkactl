package com.michelin.kafkactl.command;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Output;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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

    @Parameters(
        index = "0",
        description = "Resource type or 'all' to display resources of all types.",
        arity = "1")
    public String resourceType;

    @Parameters(
        index = "1",
        description = "Resource name or wildcard matching resource names.",
        arity = "0..1",
        defaultValue = "*")
    public String resourceName;

    @Option(
        names = {"--search"},
        description = "Search resources based on parameters.",
        arity = "0..1",
        split = ",")
    public Map<String, String> search;

    @Option(
        names = {"-o", "--output"},
        description = "Output format (${COMPLETION-CANDIDATES}).",
        defaultValue = "table")
    public Output output;

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

        try {
            return resourceService.list(apiResources, getNamespace(), resourceName, search, output, commandSpec);
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, apiResources.getFirst().getKind(), resourceName, commandSpec);
            return 1;
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
