package com.michelin.kafkactl.command.config;

import static com.michelin.kafkactl.service.FormatService.Output.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.service.FormatService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

/**
 * Config get contexts subcommand test.
 */
@ExtendWith(MockitoExtension.class)
class ConfigGetContextsTest {
    @Mock
    KafkactlConfig kafkactlConfig;

    @Mock
    FormatService formatService;

    @InjectMocks
    ConfigGetContexts subcommand;

    @Test
    void shouldGetEmptyContexts() {
        when(kafkactlConfig.getContexts())
            .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        assertTrue(sw.toString().contains("No context pre-defined."));
    }

    @Test
    void shouldGetContextsWithMaskedTokens() {
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

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        verify(formatService).displayList(eq("Context"),
            argThat(currentContext -> currentContext.getFirst().getMetadata().getName().equals("name")
                && currentContext.getFirst().getSpec().get("token").equals("[MASKED]")),
            eq(TABLE), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldGetContextsWithUnmaskedTokens() {
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

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-u");
        assertEquals(0, code);
        verify(formatService).displayList(eq("Context"),
            argThat(currentContext -> currentContext.getFirst().getMetadata().getName().equals("name")
                && currentContext.getFirst().getSpec().get("token").equals("userToken")),
            eq(TABLE), eq(cmd.getCommandSpec()));
    }
}
