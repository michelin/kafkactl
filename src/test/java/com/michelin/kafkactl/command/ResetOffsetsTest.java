package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.command.ResetOffsets.OPTIONS;
import static com.michelin.kafkactl.command.ResetOffsets.RESET_METHOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ResetOffsetsTest {
    @Mock
    LoginService loginService;

    @Mock
    ResourceService resourceService;

    @Mock
    ConfigService configService;

    @Mock
    KafkactlConfig kafkactlConfig;

    @Mock
    Kafkactl kafkactl;

    @InjectMocks
    ResetOffsets resetOffsets;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(resetOffsets);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(false);

        int code = cmd.execute("--group", "myGroup", "--all-topics", "--to-earliest");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("No valid current context found. "
            + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotResetWhenNotAuthenticated() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(false);

        CommandLine cmd = new CommandLine(resetOffsets);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("--group", "myGroup", "--all-topics", "--to-earliest");
        assertEquals(1, code);
    }

    @ParameterizedTest
    @CsvSource({"--to-earliest,TO_EARLIEST",
        "--to-latest,TO_LATEST",
        "--to-datetime=2000-01-01T12:00:00Z,TO_DATETIME",
        "--shift-by=10,SHIFT_BY"})
    void shouldResetOffsetsToEarliest(String method, String expectedMethod) {
        kafkactl.optionalNamespace = Optional.of("namespace");

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsets);

        int code = cmd.execute("--group", "myGroup", "--topic", "myTopic", method);
        assertEquals(0, code);
        verify(resourceService).resetOffsets(eq("namespace"), eq("myGroup"),
            argThat(resource -> resource.getSpec().get(RESET_METHOD).equals(expectedMethod)),
            eq(false), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldResetOffsetsToEarliestDryRun() {
        kafkactl.optionalNamespace = Optional.of("namespace");

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsets);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("--group", "myGroup", "--topic", "myTopic", "--to-earliest", "--dry-run");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
        verify(resourceService).resetOffsets(eq("namespace"), eq("myGroup"),
            argThat(resource -> resource.getSpec().get(RESET_METHOD).equals("TO_EARLIEST")),
            eq(true), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldResetOffsetsByDuration() {
        kafkactl.optionalNamespace = Optional.of("namespace");

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsets);

        int code = cmd.execute("--group", "myGroup", "--topic", "myTopic", "--by-duration=PT10M");
        assertEquals(0, code);
        verify(resourceService).resetOffsets(eq("namespace"), eq("myGroup"),
            argThat(resource -> resource.getSpec().get(RESET_METHOD).equals("BY_DURATION")
                && resource.getSpec().get(OPTIONS).equals("PT10M")),
            eq(false), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldResetOffsetsToOffset() {
        kafkactl.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsets);

        int code = cmd.execute("--group", "myGroup", "--topic", "myTopic", "--to-offset=50");
        assertEquals(0, code);
        verify(resourceService).resetOffsets(eq("namespace"), eq("myGroup"),
            argThat(resource -> resource.getSpec().get(RESET_METHOD).equals("TO_OFFSET")
                && resource.getSpec().get(OPTIONS).equals(50)),
            eq(false), eq(cmd.getCommandSpec()));
    }
}
