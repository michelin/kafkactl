package com.michelin.kafkactl;

import com.michelin.kafkactl.services.ConfigService;
import com.michelin.kafkactl.services.FormatService;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigSubcommandTest {
    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public ConfigService configService;

    @Mock
    public FormatService formatService;

    @Mock
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ConfigSubcommand configSubcommand;

    @Test
    void shouldGetCurrentContext() {
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(kafkactlConfig.getApi())
                .thenReturn("ns4kafka.com");
        when(kafkactlConfig.getUserToken())
                .thenReturn("user-token");
        when(configService.getCurrentContextName())
                .thenReturn("current-context");
        doAnswer(answer -> {
            PrintWriter cdmPrintWriter = answer.getArgument(3, PrintWriter.class);
            cdmPrintWriter.println("Called display list");
            return null;
        }).when(formatService).displayList(any(), any(), any(), any());

        CommandLine cmd = new CommandLine(configSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("current-context");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Called display list"));
    }

    @Test
    void shouldGetEmptyContexts() {
        when(kafkactlConfig.getContexts())
                .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(configSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("get-contexts");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("No context pre-defined."));
    }

    @Test
    void shouldGetContexts() {
        KafkactlConfig.Context context = KafkactlConfig.Context.builder()
                .name("name")
                .definition(KafkactlConfig.Context.ApiContext.builder()
                        .api("api")
                        .namespace("namespace")
                        .userToken("userToken")
                        .build())
                .build();

        when(kafkactlConfig.getContexts())
                .thenReturn(Collections.singletonList(context));
        doAnswer(answer -> {
            PrintWriter cdmPrintWriter = answer.getArgument(3, PrintWriter.class);
            cdmPrintWriter.println("Called display list");
            return null;
        }).when(formatService).displayList(any(), any(), any(), any());

        CommandLine cmd = new CommandLine(configSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("get-contexts");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Called display list"));
    }

    @Test
    void shouldUseContextNotFound() {
        KafkactlConfig.Context context = KafkactlConfig.Context.builder()
                .name("name")
                .definition(KafkactlConfig.Context.ApiContext.builder()
                        .api("api")
                        .namespace("namespace")
                        .userToken("userToken")
                        .build())
                .build();

        when(kafkactlConfig.getContexts())
                .thenReturn(Collections.singletonList(context));
        when(configService.getContextByName(any()))
                .thenReturn(Optional.empty());

        CommandLine cmd = new CommandLine(configSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("use-context", "context");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("No context exists with the name: context"));
    }

    @Test
    void shouldUseContext() throws IOException {
        KafkactlConfig.Context context = KafkactlConfig.Context.builder()
                .name("name")
                .definition(KafkactlConfig.Context.ApiContext.builder()
                        .api("api")
                        .namespace("namespace")
                        .userToken("userToken")
                        .build())
                .build();

        when(kafkactlConfig.getContexts())
                .thenReturn(Collections.singletonList(context));
        when(configService.getContextByName(any()))
                .thenReturn(Optional.of(context));
        doNothing().when(configService)
                .updateConfigurationContext(any());

        CommandLine cmd = new CommandLine(configSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("use-context", "context");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Switched to context \"context\"."));
    }
}
