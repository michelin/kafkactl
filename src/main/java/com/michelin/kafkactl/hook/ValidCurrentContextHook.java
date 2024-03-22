package com.michelin.kafkactl.hook;

import com.michelin.kafkactl.command.config.ConfigSubcommand;
import com.michelin.kafkactl.command.config.ConfigUseContextSubcommand;
import com.michelin.kafkactl.service.ConfigService;
import jakarta.inject.Inject;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Command hook to check if the current context is valid.
 */
@CommandLine.Command
public class ValidCurrentContextHook implements Callable<Integer> {
    @Inject
    public ConfigService configService;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() throws Exception {
        if (!configService.isCurrentContextValid()) {
            CommandLine configSubcommand = new CommandLine(new ConfigSubcommand());
            CommandLine configUseContextSubcommand = new CommandLine(new ConfigUseContextSubcommand());
            commandSpec.commandLine().getErr().println("No valid current context found. Use \"kafkactl "
                + configSubcommand.getCommandName() + " "
                + configUseContextSubcommand.getCommandName() + "\" to set a valid context.");
            return 1;
        }
        return 0;
    }
}
