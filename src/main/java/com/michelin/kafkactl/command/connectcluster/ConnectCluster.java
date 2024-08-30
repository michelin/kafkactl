package com.michelin.kafkactl.command.connectcluster;

import com.michelin.kafkactl.hook.HelpHook;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Connect clusters subcommand.
 */
@Command(name = "connect-cluster",
    subcommands = {
        ConnectClusterVault.class,
    },
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    synopsisSubcommandLabel = "COMMAND",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Interact with connect clusters.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true)
public class ConnectCluster extends HelpHook {
    @Spec
    public CommandSpec commandSpec;
}
