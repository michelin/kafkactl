package com.michelin.kafkactl.hook;

import com.michelin.kafkactl.command.config.Config;
import com.michelin.kafkactl.command.config.ConfigUseContext;
import com.michelin.kafkactl.service.ConfigService;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Command hook to check if the current context is valid.
 */
@CommandLine.Command
public abstract class ValidCurrentContextHook implements Callable<Integer> {
    @Inject
    public ConfigService configService;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() throws Exception {
        if (!configService.isCurrentContextValid()) {
            CommandLine configSubcommand = new CommandLine(new Config());
            CommandLine configUseContextSubcommand = new CommandLine(new ConfigUseContext());
            commandSpec.commandLine().getErr().println("No valid current context found. Use \"kafkactl "
                + configSubcommand.getCommandName() + " "
                + configUseContextSubcommand.getCommandName() + "\" to set a valid context.");
            return 1;
        }
        return onContextValid();
    }

    public abstract Integer onContextValid() throws IOException;
}
