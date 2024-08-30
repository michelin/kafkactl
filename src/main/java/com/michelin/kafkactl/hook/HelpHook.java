package com.michelin.kafkactl.hook;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Help abstract command.
 */
@Command
public abstract class HelpHook {
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    public boolean help;
}
