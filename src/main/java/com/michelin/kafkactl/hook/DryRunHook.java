package com.michelin.kafkactl.hook;

import java.io.IOException;
import picocli.CommandLine;

/**
 * Dry run abstract command.
 */
@CommandLine.Command
public abstract class DryRunHook extends AuthenticatedHook {
    @CommandLine.Option(names = {"--dry-run"}, description = "Does not persist resources. Validate only.")
    public boolean dryRun;

    @Override
    public Integer onContextValid() throws IOException {
        if (dryRun) {
            commandSpec.commandLine().getOut().println("Dry run execution.");
        }

        return super.onContextValid();
    }
}
