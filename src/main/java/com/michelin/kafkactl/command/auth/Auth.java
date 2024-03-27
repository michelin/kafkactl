package com.michelin.kafkactl.command.auth;

import com.michelin.kafkactl.util.VersionProvider;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Auth subcommand.
 */
@Command(name = "auth",
    subcommands = {
        AuthInfo.class,
        AuthRenew.class
    },
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    synopsisSubcommandLabel = "COMMAND",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Interact with authentication.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class Auth {
    @Spec
    public CommandSpec commandSpec;
}
