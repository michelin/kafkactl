package com.michelin.kafkactl.utils;

import com.michelin.kafkactl.KafkactlConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

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
