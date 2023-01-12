package com.michelin.kafkactl;

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.NamespacedResourceClient;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "get", description = {
        "Get resources by resource type for the current namespace",
        "Examples:",
        "  kafkactl get topic topic1 : Display topic1 configuration",
        "  kafkactl get topics : Display all topics",
        "  kafkactl get all : Display all resources",
        "Parameters: "
})
public class GetSubcommand implements Callable<Integer> {
    @Inject
    public NamespacedResourceClient namespacedClient;

    @Inject
    public ClusterResourceClient nonNamespacedClient;

    @Inject
    public LoginService loginService;

    @Inject
    public ApiResourcesService apiResourcesService;

    @Inject
    public ResourceService resourceService;

    @Inject
    public FormatService formatService;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @Parameters(index = "0", description = "Resource type or 'all' to display resources for all types", arity = "1")
    public String resourceType;

    @Parameters(index = "1", description = "Resource name", arity = "0..1")
    public Optional<String> resourceName;

    @Option(names = {"-o", "--output"}, description = "Output format. One of: yaml|table", defaultValue = "table")
    public String output;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "get" command
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer call() throws Exception {
        boolean authenticated = loginService.doAuthenticate();
        if (!authenticated) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Login failed");
        }

        // Validate resourceType + custom type ALL
        List<ApiResource> apiResources = validateResourceType();

        validateOutput();

        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        // List resources based on parameters
        if (resourceName.isEmpty() || apiResources.size() > 1) {
            try {
                // List all resources for given types (k get all, k get topics)
                Map<ApiResource, List<Resource>> resources = resourceService.listAll(apiResources, namespace);

                if (resources.entrySet().size() == 1 && resources.get(resources.keySet().iterator().next()).isEmpty()) {
                    System.out.println("No resource to display.");
                } else {
                    // Display all resources by type
                    resources.entrySet()
                            .stream()
                            .filter(kv -> !kv.getValue().isEmpty())
                            .forEach(kv -> formatService.displayList(kv.getValue().get(0).getKind(), kv.getValue(), output));
                }
            } catch (HttpClientResponseException e) {
                formatService.displayError(e, apiResources.get(0).getKind(), null);
            } catch (Exception e) {
                System.out.println("Error during get for resource type " + resourceType + ": " + e.getMessage());
            }
        } else {
            try {
                // Get individual resources for given types (k get topic topic1)
                Resource singleResource = resourceService.getSingleResourceWithType(apiResources.get(0), namespace, resourceName.get(), true);
                formatService.displaySingle(singleResource, output);
            } catch (HttpClientResponseException e) {
                formatService.displayError(e, apiResources.get(0).getKind(), resourceName.get());
            } catch (Exception e) {
                System.out.println("Error during get for resource type " + apiResources.get(0).getKind() + "/" + resourceName.get() + ": " + e.getMessage());
            }

        }

        return 0;
    }

    /**
     * Validate required output format
     */
    private void validateOutput() {
        if (!List.of("table", "yaml").contains(output)) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Invalid value " + output + " for option -o");
        }
    }

    /**
     * Validate required resource type
     * @return The list of resource type
     */
    private List<ApiResource> validateResourceType() {
        // Specific case ALL
        if (resourceType.equalsIgnoreCase("ALL")) {
            return apiResourcesService.getListResourceDefinition()
                    .stream()
                    .filter(ApiResource::isNamespaced)
                    .collect(Collectors.toList());
        }

        // Otherwise, check resource exists
        Optional<ApiResource> optionalApiResource = apiResourcesService.getResourceDefinitionFromCommandName(resourceType);
        if (optionalApiResource.isPresent()) {
            return List.of(optionalApiResource.get());
        }

        throw new CommandLine.ParameterException(commandSpec.commandLine(), "The server doesn't have resource type " + resourceType);
    }
}
