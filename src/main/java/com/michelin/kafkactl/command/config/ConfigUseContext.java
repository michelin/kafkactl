package com.michelin.kafkactl.command.config;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.hook.HelpHook;
import com.michelin.kafkactl.service.ConfigService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * Config use context subcommand.
 */
@Command(name = "use-context",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Use a context.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true)
public class ConfigUseContext extends HelpHook implements Callable<Integer> {
    @Inject
    @ReflectiveAccess
    private KafkactlConfig kafkactlConfig;

    @Inject
    @ReflectiveAccess
    private ConfigService configService;

    @Parameters(index = "0", defaultValue = "", description = "Context to use.", arity = "1")
    public String context;

    @Spec
    public CommandSpec commandSpec;

    @Override
    public Integer call() throws IOException {
        if (kafkactlConfig.getContexts().isEmpty()) {
            commandSpec.commandLine().getOut().println("No context pre-defined.");
            return 0;
        }

        Optional<KafkactlConfig.Context> optionalContextToSet = configService.getContextByName(context);
        if (optionalContextToSet.isEmpty()) {
            commandSpec.commandLine().getErr().println("No context exists with the name \"" + context + "\".");
            return 1;
        }

        KafkactlConfig.Context contextToSet = optionalContextToSet.get();
        configService.updateConfigurationContext(contextToSet);
        commandSpec.commandLine().getOut().println("Switched to context \"" + context + "\".");

        return 0;
    }
}
