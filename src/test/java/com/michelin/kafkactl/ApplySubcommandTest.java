package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.*;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.File;
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
    private FileService fileService;

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
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(false);

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(1, code);
        assertTrue(sw.toString().contains("Login failed."));
    }

    @Test
    void shouldNotApplyWhenNoFileInStdin() {
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Required one of -f or stdin."));
    }

    @Test
    void shouldNotApplyWhenYmlFileNotFound() {
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

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

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.singletonList(resource));

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

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

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
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
    void shouldNotApplyNullResponse() {
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

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(Collections.singletonList(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(null);

        CommandLine cmd = new CommandLine(applySubcommand);

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(1, code);
        verify(formatService).displayError("Cannot deploy resource", "Topic", "prefix.topic", cmd.getCommandSpec());
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

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(Collections.singletonList(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse
                        .ok(resource)
                        .header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Success Topic/prefix.topic (Created)."));
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

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(Collections.singletonList(apiResource));
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
        assertTrue(sw.toString().contains("Success Topic/prefix.topic (Created)."));
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

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(Collections.singletonList(apiResource));
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
        assertTrue(sw.toString().contains("Success Schema/prefix.schema (Created)."));
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

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(Collections.singletonList(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse
                        .ok(resource));

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Success Schema/prefix.schema."));
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

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(Collections.singletonList(apiResource));

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Cannot open schema file src/test/resources/not-exist.avsc. Schema path must be relative to the CLI."));
    }
}
