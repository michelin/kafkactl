package com.michelin.kafkactl.command;

import com.michelin.kafkactl.hook.DryRunHook;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Delete records subcommand.
 */
@Command(name = "delete-records",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Delete all records within a topic.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true)
public class DeleteRecords extends DryRunHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Parameters(description = "Name of the topic.", arity = "1")
    public String topic;

    /**
     * Run the "delete-records" command.
     *
     * @return The command return code
     */
    @Override
    public Integer onAuthSuccess() {
        return resourceService.deleteRecords(getNamespace(), topic, dryRun, commandSpec);
    }
}
