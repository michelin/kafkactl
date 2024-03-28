package com.michelin.kafkactl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class KafkactlTest {
    @InjectMocks
    Kafkactl kafkactl;

    @Test
    void shouldSetVerbose() {
        CommandLine cmd = new CommandLine(kafkactl);
        int code = cmd.execute();
        assertEquals(0, code);
    }
}
