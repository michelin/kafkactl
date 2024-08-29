package com.michelin.kafkactl.mixin;

import picocli.CommandLine.Option;

/**
 * Unmask token mixin.
 */
public class UnmaskTokenMixin {
    public static final String MASKED = "[MASKED]";

    @Option(names = {"-u", "--unmask-tokens"}, description = "Unmask tokens.")
    public boolean unmaskTokens;
}
