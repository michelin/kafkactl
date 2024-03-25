package com.michelin.kafkactl.hook;

import java.io.IOException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Dry run abstract command.
 */
@Command
public abstract class DryRunHook extends AuthenticatedHook {
    @Option(names = {"--dry-run"}, description = "Does not persist resources. Validate only.")
    public boolean dryRun;

    @Override
    public Integer onContextValid() throws IOException {
        if (dryRun) {
            commandSpec.commandLine().getOut().println("Dry run execution.");
        }

        return super.onContextValid();
    }
}
