package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteSubcommandTest {
    @Mock
    private KafkactlConfig kafkactlConfig;

    @Mock
    private ResourceService resourceService;

    @Mock
    private LoginService loginService;

    @Mock
    public FormatService formatService;

    @Mock
    private ApiResourcesService apiResourcesService;

    @Mock
    private FileService fileService;

    @Mock
    private KafkactlCommand kafkactlCommand;

    @InjectMocks
    private DeleteSubcommand deleteSubcommand;

    @Test
    void shouldNotDeleteByNameWhenNotAuthenticated() {
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(false);

        CommandLine cmd = new CommandLine(deleteSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic", "myTopic");
        assertEquals(1, code);
    }

    @Test
    void shouldNotDeleteByFileWhenYmlFileNotFound() {
        kafkactlCommand.optionalNamespace = Optional.empty();
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(deleteSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Could not find YAML or YML files in topic directory."));
    }

    @Test
    void shouldNotDeleteByFileWhenInvalidResources() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.singletonList(resource));

        CommandLine cmd = new CommandLine(deleteSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type(s) Topic."));
    }

    @Test
    void shouldNotDeleteByNameWhenInvalidResources() {
        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByCommandName(any()))
                .thenReturn(Optional.empty());

        CommandLine cmd = new CommandLine(deleteSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic", "myTopic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type(s) topic."));
    }

    @Test
    void shouldNotDeleteByFileWhenNamespaceMismatch() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespaceMismatch");
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(deleteSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Namespace mismatch between Kafkactl and YAML document Topic/prefix.topic."));
    }

    @Test
    void shouldDeleteByFileSuccess() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(apiResourcesService.getResourceDefinitionByKind(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(deleteSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic");
        assertEquals(0, code);
    }

    @Test
    void shouldDeleteByNameSuccess() {
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
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(apiResourcesService.getResourceDefinitionByKind(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(deleteSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topic", "prefix.topic");
        assertEquals(0, code);
    }

    @Test
    void shouldDeleteByFileDryRunSuccess() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(apiResourcesService.getResourceDefinitionByKind(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(deleteSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic", "--dry-run");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
    }

    @Test
    void shouldDeleteByFileFail() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(apiResourcesService.getResourceDefinitionByKind(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(false);

        CommandLine cmd = new CommandLine(deleteSubcommand);

        int code = cmd.execute("-f", "topic");
        assertEquals(1, code);
    }

    @Test
    void shouldNotDeleteByFileWhenHttpClientResponseException() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(apiResourcesService.getResourceDefinitionByKind(any()))
                .thenThrow(exception);

        CommandLine cmd = new CommandLine(deleteSubcommand);

        int code = cmd.execute("-f", "topic");
        assertEquals(1, code);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }
}
