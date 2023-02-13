package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.*;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "apply", description = "Create or update a resource.")
public class ApplySubcommand implements Callable<Integer> {
    public static final String SCHEMA_FILE = "schemaFile";

    @Inject
    public LoginService loginService;

    @Inject
    public ApiResourcesService apiResourcesService;

    @Inject
    public FormatService formatService;

    @Inject
    public FileService fileService;

    @Inject
    public ResourceService resourceService;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @Option(names = {"-f", "--file"}, description = "YAML file or directory containing resources.")
    public Optional<File> file;

    @Option(names = {"-R", "--recursive"}, description = "Search file recursively.")
    public boolean recursive;

    @Option(names = {"--dry-run"}, description = "Does not persist resources. Validate only.")
    public boolean dryRun;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "apply" command
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer call() throws Exception {
        if (dryRun) {
            commandSpec.commandLine().getOut().println("Dry run execution.");
        }

        if (!loginService.doAuthenticate(commandSpec, kafkactlCommand.verbose)) {
            return 1;
        }

        // If we have none or both stdin and File set, we stop
        boolean hasStdin = System.in.available() > 0;
        if (hasStdin == file.isPresent()) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Required one of -f or stdin.");
        }

        List<Resource> resources = resourceService.parseResources(file, recursive, commandSpec);

        try {
            // Validate resource types from resources
            List<Resource> invalidResources = apiResourcesService.validateResourceTypes(resources);
            if (!invalidResources.isEmpty()) {
                String invalid = invalidResources
                        .stream()
                        .map(Resource::getKind)
                        .distinct()
                        .collect(Collectors.joining(", "));
                throw new CommandLine.ParameterException(commandSpec.commandLine(), "The server does not have resource type(s) " + invalid + ".");
            }

            // Validate namespace mismatch
            String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());
            List<Resource> namespaceMismatch = resources
                    .stream()
                    .filter(resource -> resource.getMetadata().getNamespace() != null && !resource.getMetadata().getNamespace().equals(namespace))
                    .collect(Collectors.toList());

            if (!namespaceMismatch.isEmpty()) {
                String invalid = namespaceMismatch
                        .stream()
                        .map(resource -> resource.getKind() + "/" + resource.getMetadata().getName())
                        .distinct()
                        .collect(Collectors.joining(", "));
                throw new CommandLine.ParameterException(commandSpec.commandLine(), "Namespace mismatch between Kafkactl and YAML document " + invalid + ".");
            }

            // Load schema content
            resources
                    .stream()
                    .filter(resource -> resource.getKind().equals("Schema") && StringUtils.isNotEmpty((CharSequence) resource.getSpec().get(SCHEMA_FILE)))
                    .forEach(resource -> {
                        try {
                            resource.getSpec().put("schema", Files.readString(new File(resource.getSpec().get(SCHEMA_FILE).toString()).toPath()));
                        } catch (Exception e) {
                            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Cannot open schema file " + resource.getSpec().get(SCHEMA_FILE) +
                                    ". Schema path must be relative to the CLI.");
                        }
                    });

            int errors = resources.stream()
                    .map(resource -> {
                        ApiResource apiResource = apiResourcesService.getResourceDefinitionByKind(resource.getKind()).orElseThrow();
                        return resourceService.apply(apiResource, namespace, resource, dryRun, commandSpec);
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
