package com.michelin.kafkactl.util;

import com.michelin.kafkactl.config.KafkactlConfig;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

/**
 * Version provider.
 */
@Singleton
public class VersionProvider implements CommandLine.IVersionProvider {
    @Inject
    @ReflectiveAccess
    private KafkactlConfig kafkactlConfig;

    @Override
    public String[] getVersion() {
        return new String[] {"Version " + kafkactlConfig.getVersion()};
    }
}
