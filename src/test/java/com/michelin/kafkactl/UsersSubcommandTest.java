package com.michelin.kafkactl;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersSubcommandTest {
    @Mock
    public LoginService loginService;

    @Mock
    public ResourceService resourceService;

    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private UsersSubcommand usersSubcommand;

    @Test
    void shouldNotUpdateUserWhenNotAuthenticated() {
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(false);

        CommandLine cmd = new CommandLine(usersSubcommand);

        int code = cmd.execute("user");
        assertEquals(1, code);
    }

    @Test
    void shouldNotUpdateUserWhenUnknownOutput() {
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        CommandLine cmd = new CommandLine(usersSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("user", "-o", "unknownOutputFormat");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Invalid value unknownOutputFormat for option -o."));
    }

    @Test
    void shouldNotUpdateUserWhenNotConfirmed() {
        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");

        CommandLine cmd = new CommandLine(usersSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("user");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("You are about to change your Kafka password for the namespace namespace."));
        assertTrue(sw.toString().contains("Active connections will be killed instantly."));
        assertTrue(sw.toString().contains("To execute this operation, rerun the command with option --execute."));
    }

    @Test
    void shouldUpdateUser() {
        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");
        when(resourceService.resetPassword(any(), any(), any(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(usersSubcommand);

        int code = cmd.execute("user", "--execute");
        assertEquals(0, code);
        verify(resourceService).resetPassword("namespace", "user", TABLE, cmd.getCommandSpec());
    }
}
