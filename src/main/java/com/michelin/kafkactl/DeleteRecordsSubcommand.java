package com.michelin.kafkactl;

import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "delete-records", description = "Delete all records within a topic.")
public class DeleteRecordsSubcommand implements Callable<Integer> {
    @Inject
    public LoginService loginService;

    @Inject
    public ResourceService resourceService;

    @Inject
    public FormatService formatService;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @Parameters(description = "Name of the topic.", arity = "1")
    public String topic;

    @Option(names = {"--dry-run"}, description = "Does not persist resources. Validate only.")
    public boolean dryRun;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "delete-records" command
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer call() throws Exception {
        if (dryRun) {
            commandSpec.commandLine().getOut().println("Dry run execution.");
        }

        if (!loginService.doAuthenticate(commandSpec, kafkactlCommand.verbose)) {
            return 1;
        }

        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());
        return resourceService.deleteRecords(namespace, topic, dryRun, commandSpec);
    }
}
