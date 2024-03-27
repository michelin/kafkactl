package com.michelin.kafkactl.command.config;

import com.michelin.kafkactl.util.VersionProvider;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;


/**
 * Config subcommand.
 */
@Command(name = "config",
    subcommands = {
        ConfigGetContexts.class,
        ConfigUseContext.class,
        ConfigCurrentContext.class
    },
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    synopsisSubcommandLabel = "COMMAND",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Manage configuration.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class Config {
    @Spec
    public CommandSpec commandSpec;
}
