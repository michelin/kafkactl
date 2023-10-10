package com.michelin.kafkactl.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.config.KafkactlConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VersionProviderTest {
    @Mock
    public KafkactlConfig kafkactlConfig;

    @InjectMocks
    public VersionProvider versionProvider;

    @Test
    void shouldGetVersion() {
        when(kafkactlConfig.getVersion())
            .thenReturn("1.0.0");

        String[] actual = versionProvider.getVersion();

        assertEquals("Version 1.0.0", actual[0]);
    }
}
