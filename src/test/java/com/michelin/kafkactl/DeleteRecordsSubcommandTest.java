package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.FormatService;
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
import java.util.Collections;
import java.util.Optional;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteRecordsSubcommandTest {
    @Mock
    public LoginService loginService;

    @Mock
    public ResourceService resourceService;

    @Mock
    public FormatService formatService;

    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private DeleteRecordsSubcommand deleteRecordsSubcommand;

    @Test
    void shouldNotDeleteWhenNotAuthenticated() {
        when(loginService.doAuthenticate())
                .thenReturn(false);

        CommandLine cmd = new CommandLine(deleteRecordsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Login failed."));
    }

    @Test
    void shouldDeleteDryRun() {
        Resource resource = Resource.builder()
                .kind("DeleteRecordsResponse")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.connector")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(resourceService.deleteRecords(any(), any(), anyBoolean()))
                .thenReturn(Collections.singletonList(resource));

        CommandLine cmd = new CommandLine(deleteRecordsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topic", "--dry-run");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
        verify(formatService).displayList(eq("DeleteRecordsResponse"),
                argThat(deleteRecords -> deleteRecords.get(0).equals(resource)),
                eq(TABLE), eq(cmd.getOut()));
    }

    @Test
    void shouldDelete() {
        Resource resource = Resource.builder()
                .kind("DeleteRecordsResponse")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.connector")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(resourceService.deleteRecords(any(), any(), anyBoolean()))
                .thenReturn(Collections.singletonList(resource));

        CommandLine cmd = new CommandLine(deleteRecordsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topic");
        assertEquals(0, code);
        verify(formatService).displayList(eq("DeleteRecordsResponse"),
                argThat(deleteRecords -> deleteRecords.get(0).equals(resource)),
                eq(TABLE), eq(cmd.getOut()));
    }

    @Test
    void shouldDeleteEmptyResult() {
        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(resourceService.deleteRecords(any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(deleteRecordsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topic");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("No records to delete for the topic topic."));
    }
}
