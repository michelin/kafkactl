package com.michelin.kafkactl;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.*;
import com.michelin.kafkactl.utils.VersionProvider;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.michelin.kafkactl.ApplySubcommand.SCHEMA_FILE;

@Command(name = "diff",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@:%n%n",
        description = "Get differences between a new resource and a old resource.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true,
        versionProvider = VersionProvider.class,
        mixinStandardHelpOptions = true)
public class DiffSubcommand implements Callable<Integer> {
    @Inject
    public LoginService loginService;

    @Inject
    public ApiResourcesService apiResourcesService;

    @Inject
    public FileService fileService;

    @Inject
    public ResourceService resourceService;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @Inject
    public FormatService formatService;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @Option(names = {"-f", "--file"}, description = "YAML file or directory containing resources.")
    public Optional<File> file;

    @Option(names = {"-R", "--recursive"}, description = "Search file recursively.")
    public boolean recursive;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "diff" command
     * @return The command return code
     */
    @Override
    public Integer call() throws Exception {
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
            resources.stream()
                    .filter(resource -> resource.getKind().equals("Schema") && StringUtils.isNotEmpty((CharSequence) resource.getSpec().get(SCHEMA_FILE)))
                    .forEach(resource -> {
                        try {
                            resource.getSpec().put("schema", Files.readString(new File(resource.getSpec().get(SCHEMA_FILE).toString()).toPath()));
                        } catch (Exception e) {
                            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Cannot open schema file " + resource.getSpec().get(SCHEMA_FILE) +
                                    ". Schema path must be relative to the CLI.");
                        }
                    });

            // Process each document individually, return 0 when all succeed
            int errors = resources.stream()
                    .map(resource -> {
                        ApiResource apiResource = apiResourcesService.getResourceDefinitionByKind(resource.getKind()).orElseThrow();
                        Resource live = resourceService.getSingleResourceWithType(apiResource, namespace, resource.getMetadata().getName(), false);
                        HttpResponse<Resource> merged = resourceService.apply(apiResource, namespace, resource, true, commandSpec);
                        if (merged != null && merged.getBody().isPresent()) {
                            List<String> uDiff = unifiedDiff(live, merged.body());
                            uDiff.forEach(diff -> commandSpec.commandLine().getOut().println(diff));
                            return 0;
                        }
                        return 1;
                    })
                    .mapToInt(value -> value)
                    .sum();

            return errors > 0 ? 1 : 0;
        } catch (HttpClientResponseException e) {
            formatService.displayError(e, commandSpec);
            return 1;
        }
    }

    /**
     * Compute the difference between current resource and applied resource
     * @param live The current resource
     * @param merged The applied new resource
     * @return The differences
     */
    private List<String> unifiedDiff(Resource live, Resource merged) {
        // Ignore status and timestamp for comparison
        if (live != null) {
            live.setStatus(null);
            live.getMetadata().setCreationTimestamp(null);
        }
        merged.setStatus(null);
        merged.getMetadata().setCreationTimestamp(null);

        DumperOptions options = new DumperOptions();
        options.setExplicitStart(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer(new DumperOptions());
        representer.addClassTag(Resource.class, Tag.MAP);
        Yaml yaml = new Yaml(representer, options);

        List<String> oldResourceStr = live != null ? yaml.dump(live).lines().collect(Collectors.toList()) : List.of();
        List<String> newResourceStr = yaml.dump(merged).lines().collect(Collectors.toList());
        Patch<String> diff = DiffUtils.diff(oldResourceStr, newResourceStr);
        return UnifiedDiffUtils.generateUnifiedDiff(
                String.format("%s/%s-LIVE", merged.getKind(), merged.getMetadata().getName()),
                String.format("%s/%s-MERGED", merged.getKind(), merged.getMetadata().getName()),
                oldResourceStr, diff, 3);
    }
}
