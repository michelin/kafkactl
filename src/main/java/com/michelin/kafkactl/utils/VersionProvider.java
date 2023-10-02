package com.michelin.kafkactl.utils;

import com.michelin.kafkactl.KafkactlConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

/**
 * Version provider.
 */
@Singleton
public class VersionProvider implements CommandLine.IVersionProvider {
    @Inject
    public KafkactlConfig kafkactlConfig;

    @Override
    public String[] getVersion() {
        return new String[] {"Version " + kafkactlConfig.getVersion()};
    }
}
