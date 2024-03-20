package com.michelin.kafkactl.command.auth;

import com.michelin.kafkactl.command.KafkactlCommand;
import com.michelin.kafkactl.utils.VersionProvider;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Auth subcommand.
 */
@CommandLine.Command(name = "auth",
    subcommands = {
        AuthInfoSubcommand.class,
        AuthRenewSubcommand.class
    },
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Interact with authentication.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class AuthSubcommand implements Callable<Integer> {
    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() {
        commandSpec.commandLine().getOut().println(new CommandLine(this).getUsageMessage());
        return 0;
    }
}
