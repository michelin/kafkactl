package com.michelin.kafkactl.command.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

/**
 * Auth subcommand test.
 */
@ExtendWith(MockitoExtension.class)
class AuthTest {
    @InjectMocks
    private Auth subcommand;

    @Test
    void shouldDisplayUsageWhenNoSubcommand() {
        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Usage: auth"));
    }
}
