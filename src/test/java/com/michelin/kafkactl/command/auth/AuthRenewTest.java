package com.michelin.kafkactl.command.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.service.LoginService;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

/**
 * Auth subcommand test.
 */
@ExtendWith(MockitoExtension.class)
class AuthRenewTest {
    @Mock
    public LoginService loginService;

    @InjectMocks
    private AuthRenew subcommand;

    @Test
    void shouldSuccessWhenRenewSucceed() {
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        assertTrue(sw.toString().contains("JWT renewed successfully."));
    }

    @Test
    void shouldFailWhenRenewFailed() {
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(false);

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(1, code);
        assertTrue(sw.toString().contains("Failed to renew JWT."));
    }
}
