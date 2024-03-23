package com.michelin.kafkactl.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ImportTest {
    @Mock
    public LoginService loginService;

    @Mock
    public ResourceService resourceService;

    @Mock
    public ApiResourcesService apiResourcesService;

    @Mock
    private ConfigService configService;

    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public Kafkactl kafkactl;

    @InjectMocks
    private Import importCmd;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(importCmd);
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
    void shouldNotImportWhenNotAuthenticated() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(false);

        CommandLine cmd = new CommandLine(importCmd);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic");
        assertEquals(1, code);
    }

    @Test
    void shouldNotImportWhenServerNotHaveResourceType() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByName(any()))
            .thenReturn(Optional.empty());

        CommandLine cmd = new CommandLine(importCmd);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type topic."));
    }

    @Test
    void shouldNotImportWhenResourceNotSynchronizable() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(false)
            .build();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByName(any()))
            .thenReturn(Optional.of(apiResource));

        CommandLine cmd = new CommandLine(importCmd);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Resource of type topic is not synchronizable."));
    }

    @Test
    void shouldImportTopicResources() {
        kafkactl.optionalNamespace = Optional.of("namespace");

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(apiResourcesService.getResourceDefinitionByName(any()))
            .thenReturn(Optional.of(apiResource));
        when(resourceService.importAll(any(), any(), anyBoolean(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(importCmd);

        int code = cmd.execute("topic");
        assertEquals(0, code);
        verify(resourceService).importAll(Collections.singletonList(apiResource), "namespace", false,
            cmd.getCommandSpec());
    }

    @Test
    void shouldImportTopicResourcesDryRun() {
        kafkactl.optionalNamespace = Optional.of("namespace");

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(apiResourcesService.getResourceDefinitionByName(any()))
            .thenReturn(Optional.of(apiResource));
        when(resourceService.importAll(any(), any(), anyBoolean(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(importCmd);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topic", "--dry-run");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
        verify(resourceService).importAll(Collections.singletonList(apiResource), "namespace", true,
            cmd.getCommandSpec());
    }

    @Test
    void shouldImportAllResources() {
        kafkactl.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");

        ApiResource nonSyncApiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(false)
            .synchronizable(false)
            .build();

        ApiResource nonNamespacedApiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(false)
            .synchronizable(true)
            .build();

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(apiResourcesService.listResourceDefinitions())
            .thenReturn(List.of(apiResource, nonNamespacedApiResource, nonSyncApiResource));
        when(resourceService.importAll(any(), any(), anyBoolean(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(importCmd);

        int code = cmd.execute("all");
        assertEquals(0, code);
        verify(resourceService).importAll(List.of(apiResource, nonNamespacedApiResource), "namespace", false,
            cmd.getCommandSpec());
    }
}
