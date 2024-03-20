package com.michelin.kafkactl.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.command.DeleteRecordsSubcommand;
import com.michelin.kafkactl.command.KafkactlCommand;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class DeleteRecordsSubcommandTest {
    @Mock
    private LoginService loginService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ConfigService configService;

    @Mock
    private KafkactlConfig kafkactlConfig;

    @Mock
    private KafkactlCommand kafkactlCommand;

    @InjectMocks
    private DeleteRecordsSubcommand deleteRecordsSubcommand;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(deleteRecordsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(false);

        int code = cmd.execute("topic");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("No valid current context found. "
            + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotDeleteWhenNotAuthenticated() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(false);

        CommandLine cmd = new CommandLine(deleteRecordsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic");
        assertEquals(1, code);
    }

    @Test
    void shouldDeleteDryRunSuccess() {
        kafkactlCommand.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");
        when(resourceService.deleteRecords(any(), any(), anyBoolean(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(deleteRecordsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topic", "--dry-run");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
    }

    @Test
    void shouldDeleteSuccess() {
        kafkactlCommand.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");
        when(resourceService.deleteRecords(any(), any(), anyBoolean(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(deleteRecordsSubcommand);

        int code = cmd.execute("topic");
        assertEquals(0, code);
    }

    @Test
    void shouldDeleteFail() {
        kafkactlCommand.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");
        when(resourceService.deleteRecords(any(), any(), anyBoolean(), any()))
            .thenReturn(1);

        CommandLine cmd = new CommandLine(deleteRecordsSubcommand);

        int code = cmd.execute("topic");
        assertEquals(1, code);
    }
}
