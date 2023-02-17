package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
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
import java.util.List;
import java.util.Optional;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportSubcommandTest {
    @Mock
    public LoginService loginService;

    @Mock
    public ResourceService resourceService;

    @Mock
    public ApiResourcesService apiResourcesService;

    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ImportSubcommand importSubcommand;

    @Test
    void shouldNotImportWhenNotAuthenticated() {
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(false);

        CommandLine cmd = new CommandLine(importSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic");
        assertEquals(1, code);
    }

    @Test
    void shouldNotImportWhenServerNotHaveResourceType() {
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByCommandName(any()))
                .thenReturn(Optional.empty());

        CommandLine cmd = new CommandLine(importSubcommand);
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

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByCommandName(any()))
                .thenReturn(Optional.of(apiResource));

        CommandLine cmd = new CommandLine(importSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Resource of type topic is not synchronizable."));
    }

    @Test
    void shouldImportTopicResources() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByCommandName(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.importAll(any(), any(), anyBoolean(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(importSubcommand);

        int code = cmd.execute("topic");
        assertEquals(0, code);
        verify(resourceService).importAll(Collections.singletonList(apiResource), "namespace", false, cmd.getCommandSpec());
    }

    @Test
    void shouldImportTopicResourcesDryRun() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByCommandName(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.importAll(any(), any(), anyBoolean(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(importSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topic", "--dry-run");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
        verify(resourceService).importAll(Collections.singletonList(apiResource), "namespace", true, cmd.getCommandSpec());
    }

    @Test
    void shouldImportAllResources() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        ApiResource nonNamespacedApiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(false)
                .synchronizable(true)
                .build();

        ApiResource nonSyncApiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(false)
                .synchronizable(false)
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.listResourceDefinitions())
                .thenReturn(List.of(apiResource, nonNamespacedApiResource, nonSyncApiResource));
        when(resourceService.importAll(any(), any(), anyBoolean(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(importSubcommand);

        int code = cmd.execute("all");
        assertEquals(0, code);
        verify(resourceService).importAll(List.of(apiResource, nonNamespacedApiResource), "namespace", false, cmd.getCommandSpec());
    }
}
