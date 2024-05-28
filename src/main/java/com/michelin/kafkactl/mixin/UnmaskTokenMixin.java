package com.michelin.kafkactl.mixin;

import picocli.CommandLine;

/**
 * Unmask token mixin.
 */
public class UnmaskTokenMixin {
    public static final String MASKED = "[MASKED]";

    @CommandLine.Option(names = {"-u", "--unmask-tokens"}, description = "Unmask tokens.")
    public boolean unmaskTokens;
}
