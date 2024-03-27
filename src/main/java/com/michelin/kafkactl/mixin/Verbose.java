package com.michelin.kafkactl.mixin;

import picocli.CommandLine.Option;

/**
 * Verbose mixin.
 */
public class Verbose {
    @Option(names = {"-v", "--verbose"}, description = "Enable the verbose mode.")
    public boolean verbose;
}
