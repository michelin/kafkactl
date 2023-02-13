package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
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

import static com.michelin.kafkactl.services.FormatService.TABLE;

@Command(name = "import", description = "Import non-synchronized resources")
public class ImportSubcommand implements Callable<Integer> {
    @Inject
    public LoginService loginService;

    @Inject
    public ResourceService resourceService;

    @Inject
    public ApiResourcesService apiResourcesService;

    @Inject
    public FormatService formatService;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @Parameters(index = "0", description = "Resource type.", arity = "1")
    public String resourceType;

    @Option(names = {"--dry-run"}, description = "Does not persist resources. Validate only.")
    public boolean dryRun;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "get" command
     * @return The command return code
     */
    public Integer call() {
        if (dryRun) {
            commandSpec.commandLine().getOut().println("Dry run execution.");
        }

        if (!loginService.doAuthenticate(commandSpec, kafkactlCommand.verbose)) {
            return 1;
        }

        // Validate resourceType + custom type ALL
        List<ApiResource> apiResources = validateResourceType();

        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        return resourceService.importAll(apiResources, namespace, dryRun, commandSpec);
    }

    /**
     * Validate required resource type
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
        Optional<ApiResource> optionalApiResource = apiResourcesService.getResourceDefinitionByCommandName(resourceType);
        if (optionalApiResource.isEmpty()) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "The server does not have resource type " + resourceType + ".");
        }

        if (!optionalApiResource.get().isSynchronizable()) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Resource of type " + resourceType + " is not synchronizable.");
        }

        return List.of(optionalApiResource.get());
    }
}
