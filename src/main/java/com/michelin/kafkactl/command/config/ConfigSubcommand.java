package com.michelin.kafkactl.command.config;

import com.michelin.kafkactl.utils.VersionProvider;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Config subcommand.
 */
@CommandLine.Command(name = "config",
    subcommands = {
        ConfigGetContextsSubcommand.class,
        ConfigUseContextSubcommand.class,
        ConfigCurrentContextSubcommand.class
    },
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Manage configuration.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ConfigSubcommand implements Callable<Integer> {
    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() throws IOException {
        commandSpec.commandLine().getOut().println(new CommandLine(this).getUsageMessage());
        return 0;
    }
}
