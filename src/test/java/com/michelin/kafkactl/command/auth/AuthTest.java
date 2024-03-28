package com.michelin.kafkactl.command.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    Auth subcommand;

    @Test
    void shouldSucceedWhenParam() {
        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-h");
        assertEquals(0, code);
    }

    @Test
    void shouldFailWhenMissingRequiredSubcommandAndNoParam() {
        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(2, code);
    }
}
