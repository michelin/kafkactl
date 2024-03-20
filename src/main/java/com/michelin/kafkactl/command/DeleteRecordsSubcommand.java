package com.michelin.kafkactl.command;

import com.michelin.kafkactl.hook.DryRunCommand;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Delete records subcommand.
 */
@Command(name = "delete-records",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Delete all records within a topic.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class DeleteRecordsSubcommand extends DryRunCommand {
    @Inject
    public ResourceService resourceService;

    @Inject
    public FormatService formatService;

    @Parameters(description = "Name of the topic.", arity = "1")
    public String topic;

    /**
     * Run the "delete-records" command.
     *
     * @return The command return code
     */
    @Override
    public Integer onAuthSuccess() {
        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());
        return resourceService.deleteRecords(namespace, topic, dryRun, commandSpec);
    }
}
