package com.michelin.kafkactl.command.config;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Config use context subcommand.
 */
@CommandLine.Command(name = "use-context",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Use a context.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ConfigUseContextSubcommand implements Callable<Integer> {
    @Inject
    public KafkactlConfig kafkactlConfig;
    
    @Inject
    public ConfigService configService;

    @CommandLine.Parameters(index = "0", defaultValue = "", description = "Context to use.", arity = "1")
    public String context;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() throws IOException {
        if (kafkactlConfig.getContexts().isEmpty()) {
            commandSpec.commandLine().getOut().println("No context pre-defined.");
            return 0;
        }

        Optional<KafkactlConfig.Context> optionalContextToSet = configService.getContextByName(context);
        if (optionalContextToSet.isEmpty()) {
            commandSpec.commandLine().getErr().println("No context exists with the name: " + context);
            return 1;
        }

        KafkactlConfig.Context contextToSet = optionalContextToSet.get();
        configService.updateConfigurationContext(contextToSet);
        commandSpec.commandLine().getOut().println("Switched to context \"" + context + "\".");

        return 0;
    }
}
