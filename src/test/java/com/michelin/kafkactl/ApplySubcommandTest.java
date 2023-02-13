package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static com.michelin.kafkactl.ApplySubcommand.SCHEMA_FILE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplySubcommandTest {
    @Mock
    private LoginService loginService;

    @Mock
    private ApiResourcesService apiResourcesService;

    @Mock
    public FormatService formatService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private KafkactlConfig kafkactlConfig;

    @Mock
    private KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ApplySubcommand applySubcommand;

    @Test
    void shouldNotApplyWhenNotAuthenticated() {
        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(false);

        int code = cmd.execute();
        assertEquals(1, code);
    }

    @Test
    void shouldNotApplyWhenNoFileInStdin() {
        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);

        int code = cmd.execute();
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Required one of -f or stdin."));
    }

    @Test
    void shouldNotApplyWhenYmlFileNotFound() {
        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenThrow(new CommandLine.ParameterException(cmd.getCommandSpec().commandLine(),
                        "Could not find YAML or YML files in topic directory."));

        int code = cmd.execute("-f", "topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Could not find YAML or YML files in topic directory."));
    }

    @Test
    void shouldNotApplyWhenInvalidResources() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.singletonList(resource));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type(s) Topic."));
    }

    @Test
    void shouldNotApplyWhenNamespaceMismatch() {
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
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Namespace mismatch between Kafkactl and YAML document Topic/prefix.topic."));
    }

    @Test
    void shouldNotApplyWhenHttpClientResponseException() {
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
        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getResourceDefinitionByKind(any()))
                .thenThrow(exception);

        CommandLine cmd = new CommandLine(applySubcommand);

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(1, code);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldApply() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getResourceDefinitionByKind(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse
                        .ok(resource)
                        .header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(0, code);
        verify(resourceService).apply(apiResource, "namespace", resource, false, cmd.getCommandSpec());
    }

    @Test
    void shouldApplyDryRun() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getResourceDefinitionByKind(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse
                        .ok(resource)
                        .header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml", "--dry-run");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
        verify(resourceService).apply(apiResource, "namespace", resource, true, cmd.getCommandSpec());
    }

    @Test
    void shouldApplySchema() {
        Map<String, Object> specs = new HashMap<>();
        specs.put(SCHEMA_FILE, "src/test/resources/person.avsc");

        Resource resource = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.schema")
                        .namespace("namespace")
                        .build())
                .spec(specs)
                .build();

        ApiResource apiResource = ApiResource.builder()
                .kind("Schema")
                .namespaced(true)
                .synchronizable(false)
                .path("schemas")
                .names(List.of("schemas", "schema", "sc"))
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getResourceDefinitionByKind(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse
                        .ok(resource)
                        .header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(0, code);
        assertFalse(resource.getSpec().get("schema").toString().isBlank());
        verify(resourceService).apply(apiResource, "namespace", resource, false, cmd.getCommandSpec());
    }

    @Test
    void shouldApplyInlineSchema() {
        Map<String, Object> specs = new HashMap<>();
        specs.put("schema", "{schema}");

        Resource resource = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.schema")
                        .namespace("namespace")
                        .build())
                .spec(specs)
                .build();

        ApiResource apiResource = ApiResource.builder()
                .kind("Schema")
                .namespaced(true)
                .synchronizable(false)
                .path("schemas")
                .names(List.of("schemas", "schema", "sc"))
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getResourceDefinitionByKind(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse
                        .ok(resource));

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(0, code);
        verify(resourceService).apply(apiResource, "namespace", resource, false, cmd.getCommandSpec());
    }

    @Test
    void shouldNotApplySchemaWhenNotExist() {
        Map<String, Object> specs = new HashMap<>();
        specs.put(SCHEMA_FILE, "src/test/resources/not-exist.avsc");

        Resource resource = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.schema")
                        .namespace("namespace")
                        .build())
                .spec(specs)
                .build();

        ApiResource apiResource = ApiResource.builder()
                .kind("Schema")
                .namespaced(true)
                .synchronizable(false)
                .path("schemas")
                .names(List.of("schemas", "schema", "sc"))
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Cannot open schema file src/test/resources/not-exist.avsc. Schema path must be relative to the CLI."));
    }
}
