package com.michelin.kafkactl.command.config;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONTEXT;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.util.VersionProvider;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Config get contexts subcommand.
 */
@CommandLine.Command(name = "get-contexts",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Get all contexts.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ConfigGetContexts implements Callable<Integer> {
    @Inject
    private KafkactlConfig kafkactlConfig;

    @Inject
    private FormatService formatService;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() {
        if (kafkactlConfig.getContexts().isEmpty()) {
            commandSpec.commandLine().getOut().println("No context pre-defined.");
        } else {
            List<Resource> allContextsAsResources = new ArrayList<>();
            kafkactlConfig.getContexts().forEach(userContext -> {
                Map<String, Object> specs = new HashMap<>();
                specs.put("namespace", userContext.getDefinition().getNamespace());
                specs.put("api", userContext.getDefinition().getApi());
                specs.put("token", userContext.getDefinition().getUserToken());

                Resource currentContextAsResource = Resource.builder()
                    .metadata(Metadata.builder()
                        .name(userContext.getName())
                        .build())
                    .spec(specs)
                    .build();

                allContextsAsResources.add(currentContextAsResource);
            });

            formatService.displayList(CONTEXT, allContextsAsResources, TABLE, commandSpec);
        }

        return 0;
    }
}
