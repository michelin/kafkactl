package com.michelin.kafkactl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.command.KafkactlCommand;
import com.michelin.kafkactl.config.KafkactlConfig;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {
    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public LoginService loginService;

    @InjectMocks
    public ConfigService configService;

    @Test
    void shouldGetCurrentContextName() {
        KafkactlConfig.Context contextOne = KafkactlConfig.Context.builder()
            .name("name")
            .definition(KafkactlConfig.Context.ApiContext.builder()
                .userToken("userToken")
                .api("https://ns4kafka.com")
                .namespace("namespace")
                .build())
            .build();

        KafkactlConfig.Context contextTwo = KafkactlConfig.Context.builder()
            .name("name2")
            .definition(KafkactlConfig.Context.ApiContext.builder()
                .userToken("userToken2")
                .api("https://ns4kafka.com")
                .namespace("namespace2")
                .build())
            .build();

        when(kafkactlConfig.getContexts())
            .thenReturn(List.of(contextOne, contextTwo));
        when(kafkactlConfig.getApi())
            .thenReturn("https://ns4kafka.com");
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");
        when(kafkactlConfig.getUserToken())
            .thenReturn("userToken");

        String actual = configService.getCurrentContextName();

        assertEquals("name", actual);
    }

    @Test
    void shouldNotGetCurrentContextName() {
        KafkactlConfig.Context contextOne = KafkactlConfig.Context.builder()
            .name("name")
            .definition(KafkactlConfig.Context.ApiContext.builder()
                .userToken("userToken")
                .api("https://ns4kafka.com")
                .namespace("namespace")
                .build())
            .build();

        KafkactlConfig.Context contextTwo = KafkactlConfig.Context.builder()
            .name("name2")
            .definition(KafkactlConfig.Context.ApiContext.builder()
                .userToken("userToken2")
                .api("https://ns4kafka.com")
                .namespace("namespace2")
                .build())
            .build();

        when(kafkactlConfig.getContexts())
            .thenReturn(List.of(contextOne, contextTwo));
        when(kafkactlConfig.getApi())
            .thenReturn("https://ns4kafka.com");
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("badNamespace");

        String actual = configService.getCurrentContextName();

        assertNull(actual);
    }

    @Test
    void shouldGetContextByName() {
        KafkactlConfig.Context contextOne = KafkactlConfig.Context.builder()
            .name("name")
            .definition(KafkactlConfig.Context.ApiContext.builder()
                .userToken("userToken")
                .api("https://ns4kafka.com")
                .namespace("namespace")
                .build())
            .build();

        KafkactlConfig.Context contextTwo = KafkactlConfig.Context.builder()
            .name("name2")
            .definition(KafkactlConfig.Context.ApiContext.builder()
                .userToken("userToken2")
                .api("https://ns4kafka.com")
                .namespace("namespace2")
                .build())
            .build();

        when(kafkactlConfig.getContexts())
            .thenReturn(List.of(contextOne, contextTwo));

        Optional<KafkactlConfig.Context> actual = configService.getContextByName("name");

        assertTrue(actual.isPresent());
        assertEquals(contextOne, actual.get());
    }

    @Test
    void shouldNotGetContextByName() {
        KafkactlConfig.Context contextOne = KafkactlConfig.Context.builder()
            .name("name")
            .definition(KafkactlConfig.Context.ApiContext.builder()
                .userToken("userToken")
                .api("https://ns4kafka.com")
                .namespace("namespace")
                .build())
            .build();

        KafkactlConfig.Context contextTwo = KafkactlConfig.Context.builder()
            .name("name2")
            .definition(KafkactlConfig.Context.ApiContext.builder()
                .userToken("userToken2")
                .api("https://ns4kafka.com")
                .namespace("namespace2")
                .build())
            .build();

        when(kafkactlConfig.getContexts())
            .thenReturn(List.of(contextOne, contextTwo));

        Optional<KafkactlConfig.Context> actual = configService.getContextByName("notFound");

        assertFalse(actual.isPresent());
    }

    @Test
    void shouldUpdateConfigurationContext() throws IOException {
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlConfig.getConfigPath())
            .thenReturn("src/test/resources/config.yml");

        String backUp = Files.readString(Paths.get(kafkactlConfig.getConfigPath()));

        assertFalse(backUp.contains("  current-namespace"));

        KafkactlConfig.Context contextToSet = KafkactlConfig.Context.builder()
            .name("name")
            .definition(KafkactlConfig.Context.ApiContext.builder()
                .userToken("userToken")
                .api("https://ns4kafka.com")
                .namespace("namespace")
                .build())
            .build();

        configService.updateConfigurationContext(contextToSet);

        String actual = Files.readString(Paths.get(kafkactlConfig.getConfigPath()));

        assertTrue(actual.contains("  current-namespace: namespace"));
        assertTrue(actual.contains("  api: https://ns4kafka.com"));
        assertTrue(actual.contains("  user-token: userToken"));

        BufferedWriter writer = new BufferedWriter(new FileWriter(kafkactlConfig.getConfigPath(), false));
        writer.append(backUp);
        writer.close();
    }
}
