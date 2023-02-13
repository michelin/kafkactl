package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.michelin.kafkactl.ResetOffsetsSubcommand.OPTIONS;
import static com.michelin.kafkactl.ResetOffsetsSubcommand.RESET_METHOD;
import static com.michelin.kafkactl.services.FormatService.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetOffsetsSubcommandTest {
    @Mock
    public LoginService loginService;

    @Mock
    public ResourceService resourceService;

    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ResetOffsetsSubcommand resetOffsetsSubcommand;

    @Test
    void shouldNotResetWhenNotAuthenticated() {
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(false);

        CommandLine cmd = new CommandLine(resetOffsetsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("--group", "myGroup", "--all-topics", "--to-earliest");
        assertEquals(1, code);
    }

    @Test
    void shouldResetOffsetsToEarliest() {
        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsetsSubcommand);

        int code = cmd.execute("--group", "myGroup", "--topic", "myTopic", "--to-earliest");
        assertEquals(0, code);
        verify(resourceService).resetOffsets(eq("namespace"), eq("myGroup"),
                argThat(resource -> resource.getSpec().get(RESET_METHOD).equals("TO_EARLIEST")),
                eq(false), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldResetOffsetsToEarliestDryRun() {
        Resource resetOffset = Resource.builder()
                .metadata(ObjectMeta.builder()
                        .name("groupID")
                        .cluster("local")
                        .build())
                .spec(Map.of(
                        "topic", "topic1",
                        "method", "TO_EARLIEST"))
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsetsSubcommand);
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
    void shouldResetOffsetsToLatest() {
        Resource resetOffset = Resource.builder()
                .metadata(ObjectMeta.builder()
                        .name("groupID")
                        .cluster("local")
                        .build())
                .spec(Map.of(
                        "topic", "topic1",
                        "method", "TO_LATEST"))
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsetsSubcommand);

        int code = cmd.execute("--group", "myGroup", "--topic", "myTopic", "--to-latest");
        assertEquals(0, code);
        verify(resourceService).resetOffsets(eq("namespace"), eq("myGroup"),
                argThat(resource -> resource.getSpec().get(RESET_METHOD).equals("TO_LATEST")),
                eq(false), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldResetOffsetsToDatetime() {
        Resource resetOffset = Resource.builder()
                .metadata(ObjectMeta.builder()
                        .name("groupID")
                        .cluster("local")
                        .build())
                .spec(Map.of(
                        "topic", "topic1",
                        "method", "TO_DATETIME"))
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsetsSubcommand);

        int code = cmd.execute("--group", "myGroup", "--topic", "myTopic", "--to-datetime=2000-01-01T12:00:00Z");
        assertEquals(0, code);
        verify(resourceService).resetOffsets(eq("namespace"), eq("myGroup"),
                argThat(resource -> resource.getSpec().get(RESET_METHOD).equals("TO_DATETIME")),
                eq(false), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldResetOffsetsShiftBy() {
        Resource resetOffset = Resource.builder()
                .metadata(ObjectMeta.builder()
                        .name("groupID")
                        .cluster("local")
                        .build())
                .spec(Map.of(
                        "topic", "topic1",
                        "method", "SHIFT_BY"))
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsetsSubcommand);

        int code = cmd.execute("--group", "myGroup", "--topic", "myTopic", "--shift-by=10");
        assertEquals(0, code);
        verify(resourceService).resetOffsets(eq("namespace"), eq("myGroup"),
                argThat(resource -> resource.getSpec().get(RESET_METHOD).equals("SHIFT_BY")),
                eq(false), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldResetOffsetsByDuration() {
        Resource resetOffset = Resource.builder()
                .metadata(ObjectMeta.builder()
                        .name("groupID")
                        .cluster("local")
                        .build())
                .spec(Map.of(
                        "topic", "topic1",
                        "method", "BY_DURATION"))
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsetsSubcommand);

        int code = cmd.execute("--group", "myGroup", "--topic", "myTopic", "--by-duration=PT10M");
        assertEquals(0, code);
        verify(resourceService).resetOffsets(eq("namespace"), eq("myGroup"),
                argThat(resource -> resource.getSpec().get(RESET_METHOD).equals("BY_DURATION") &&
                        resource.getSpec().get(OPTIONS).equals("PT10M")),
                eq(false), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldResetOffsetsToOffset() {
        Resource resetOffset = Resource.builder()
                .metadata(ObjectMeta.builder()
                        .name("groupID")
                        .cluster("local")
                        .build())
                .spec(Map.of(
                        "topic", "topic1",
                        "method", "TO_OFFSET"))
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(resourceService.resetOffsets(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(resetOffsetsSubcommand);

        int code = cmd.execute("--group", "myGroup", "--topic", "myTopic", "--to-offset=50");
        assertEquals(0, code);
        verify(resourceService).resetOffsets(eq("namespace"), eq("myGroup"),
                argThat(resource -> resource.getSpec().get(RESET_METHOD).equals("TO_OFFSET") &&
                        resource.getSpec().get(OPTIONS).equals(50)),
                eq(false), eq(cmd.getCommandSpec()));
    }
}
