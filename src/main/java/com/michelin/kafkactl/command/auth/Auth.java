package com.michelin.kafkactl.command.auth;

import com.michelin.kafkactl.util.VersionProvider;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Auth subcommand.
 */
@CommandLine.Command(name = "auth",
    subcommands = {
        AuthInfo.class,
        AuthRenew.class
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
public class Auth implements Callable<Integer> {
    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() {
        commandSpec.commandLine().getOut().println(new CommandLine(this).getUsageMessage());
        return 0;
    }
}
