package com.michelin.kafkactl.command.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.service.ConfigService;
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

/**
 * Config use context subcommand test.
 */
@ExtendWith(MockitoExtension.class)
public class ConfigUseContextSubcommandTest {
    @Mock
    private KafkactlConfig kafkactlConfig;

    @Mock
    private ConfigService configService;

    @InjectMocks
    private ConfigUseContextSubcommand subcommand;

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

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("context");
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

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("context");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Switched to context \"context\"."));
    }
}
