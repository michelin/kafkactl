package com.michelin.kafkactl.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.michelin.kafkactl.command.KafkactlCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class KafkactlCommandTest {
    @InjectMocks
    private KafkactlCommand kafkactlCommand;

    @Test
    void shouldSetVerbose() {
        CommandLine cmd = new CommandLine(kafkactlCommand);
        int code = cmd.execute("-v");
        assertEquals(0, code);
        assertTrue(kafkactlCommand.verbose);
    }

    @Test
    void shouldNotSetVerbose() {
        CommandLine cmd = new CommandLine(kafkactlCommand);
        int code = cmd.execute();
        assertEquals(0, code);
        assertFalse(kafkactlCommand.verbose);
    }
}
