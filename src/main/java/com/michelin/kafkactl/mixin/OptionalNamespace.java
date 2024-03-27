package com.michelin.kafkactl.mixin;

import java.util.Optional;
import picocli.CommandLine.Option;

/**
 * Optional namespace mixin.
 */
public class OptionalNamespace {
    @Option(names = {"-n",
        "--namespace"}, description = "Override namespace defined in config or YAML resources.")
    public Optional<String> optionalNamespace;
}
