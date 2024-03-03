package com.michelin.kafkactl;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.services.ConfigService;
import com.michelin.kafkactl.services.FormatService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ConfigSubcommandTest {
    @Mock
    private KafkactlConfig kafkactlConfig;

    @Mock
    private ConfigService configService;

    @Mock
    private FormatService formatService;

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

        CommandLine cmd = new CommandLine(configSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("current-context");
        assertEquals(0, code);
        verify(formatService).displayList(eq("Context"),
            argThat(currentContext -> currentContext.get(0).getMetadata().getName().equals("current-context")),
            eq(TABLE), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldGetEmptyContexts() {
        when(kafkactlConfig.getContexts())
            .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(configSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("get-contexts");
        assertEquals(1, code);
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

        CommandLine cmd = new CommandLine(configSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("get-contexts");
        assertEquals(0, code);
        verify(formatService).displayList(eq("Context"),
            argThat(currentContext -> currentContext.get(0).getMetadata().getName().equals("name")),
            eq(TABLE), eq(cmd.getCommandSpec()));
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
    void shouldUseContext() {
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

        CommandLine cmd = new CommandLine(configSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("use-context", "context");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Switched to context \"context\"."));
    }
}
