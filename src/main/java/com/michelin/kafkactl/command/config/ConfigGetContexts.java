package com.michelin.kafkactl.command.config;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONTEXT;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.util.VersionProvider;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Config get contexts subcommand.
 */
@Command(name = "get-contexts",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Get all contexts.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ConfigGetContexts implements Callable<Integer> {
    private static final String MASKED = "[MASKED]";

    @Inject
    @ReflectiveAccess
    private KafkactlConfig kafkactlConfig;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Spec
    public CommandSpec commandSpec;

    @Option(names = {"-u", "--unmask-tokens"}, description = "Unmask tokens.")
    public boolean unmaskTokens;

    @Override
    public Integer call() {
        if (kafkactlConfig.getContexts().isEmpty()) {
            commandSpec.commandLine().getOut().println("No context pre-defined.");
        } else {
            List<Resource> contexts = kafkactlConfig.getContexts()
                .stream()
                .map(userContext -> {
                    Map<String, Object> specs = new HashMap<>();
                    specs.put("namespace", userContext.getDefinition().getNamespace());
                    specs.put("api", userContext.getDefinition().getApi());

                    if (unmaskTokens) {
                        specs.put("token", userContext.getDefinition().getUserToken());
                    } else {
                        specs.put("token", MASKED);
                    }

                    return Resource.builder()
                        .metadata(Metadata.builder()
                            .name(userContext.getName())
                            .build())
                        .spec(specs)
                        .build();
                })
                .toList();

            formatService.displayList(CONTEXT, contexts, TABLE, commandSpec);
        }

        return 0;
    }
}
