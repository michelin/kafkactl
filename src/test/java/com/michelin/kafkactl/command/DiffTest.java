package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.service.ResourceService.SCHEMA_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class DiffTest {
    @Mock
    FormatService formatService;

    @Mock
    LoginService loginService;

    @Mock
    ApiResourcesService apiResourcesService;

    @Mock
    ResourceService resourceService;

    @Mock
    ConfigService configService;

    @Mock
    KafkactlConfig kafkactlConfig;

    @Mock
    Kafkactl kafkactl;

    @InjectMocks
    Diff diff;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(false);

        int code = cmd.execute();
        assertEquals(1, code);
        assertTrue(sw.toString().contains("No valid current context found. "
            + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotDiffWhenNotAuthenticated() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(false);

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(1, code);
    }

    @Test
    void shouldNotDiffWhenNoFileInStdin() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Required one of -f or stdin."));
    }

    @Test
    void shouldNotDiffWhenYmlFileNotFound() {
        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(true);
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
    void shouldNotDiffWhenInvalidResources() {
        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .build())
            .spec(Collections.emptyMap())
            .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
            .thenReturn(Collections.singletonList(resource));
        doThrow(new CommandLine.ParameterException(cmd.getCommandSpec().commandLine(),
            "The server does not have resource type(s) Topic."))
            .when(resourceService).validateAllowedResources(any(), any());

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type(s) Topic."));
    }

    @Test
    void shouldNotDiffWhenNamespaceMismatch() {
        kafkactl.optionalNamespace = Optional.of("namespaceMismatch");

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .namespace("namespace")
                .build())
            .spec(Collections.emptyMap())
            .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
            .thenReturn(Collections.singletonList(resource));

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Namespace mismatch between Kafkactl configuration and YAML resource(s): "
            + "\"Topic/prefix.topic\"."));
    }

    @Test
    void shouldNotDiffWhenHttpClientResponseException() {
        kafkactl.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .namespace("namespace")
                .build())
            .spec(Collections.emptyMap())
            .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
            .thenReturn(Collections.singletonList(resource));
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());

        when(apiResourcesService.getResourceDefinitionByKind(any()))
            .thenThrow(exception);

        CommandLine cmd = new CommandLine(diff);

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(1, code);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldNotDiffWhenHttpClientResponseExceptionDuringApply() {
        kafkactl.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .namespace("namespace")
                .build())
            .spec(Collections.emptyMap())
            .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
            .thenReturn(Collections.singletonList(resource));
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(apiResourcesService.getResourceDefinitionByKind(any()))
            .thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .namespace("namespace")
                .build())
            .spec(Map.of(
                "replicationFactor", 3,
                "partitions", 3
            ))
            .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
            .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(null);

        CommandLine cmd = new CommandLine(diff);

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(1, code);
    }

    @Test
    void shouldNotDiffApplyHasNoBody() {
        kafkactl.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .namespace("namespace")
                .build())
            .spec(Collections.emptyMap())
            .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
            .thenReturn(Collections.singletonList(resource));
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(apiResourcesService.getResourceDefinitionByKind(any()))
            .thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .namespace("namespace")
                .build())
            .spec(Map.of(
                "replicationFactor", 3,
                "partitions", 3
            ))
            .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
            .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(HttpResponse.ok());

        CommandLine cmd = new CommandLine(diff);

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(1, code);
    }

    @Test
    void shouldDiff() {
        kafkactl.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .namespace("namespace")
                .build())
            .spec(Map.of(
                "replicationFactor", 1,
                "partitions", 1,
                "cleanup.policy", "delete"
            ))
            .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
            .thenReturn(Collections.singletonList(resource));
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(apiResourcesService.getResourceDefinitionByKind(any()))
            .thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .namespace("namespace")
                .build())
            .spec(Map.of(
                "replicationFactor", 3,
                "partitions", 3
            ))
            .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
            .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(HttpResponse
                .ok(resource)
                .header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("--- Topic/prefix.topic-LIVE"));
        assertTrue(sw.toString().contains("+++ Topic/prefix.topic-MERGED"));
        assertTrue(sw.toString().contains("@@ -8,6 +8,7 @@"));
        assertTrue(sw.toString().contains("   name: prefix.topic"));
        assertTrue(sw.toString().contains("   namespace: namespace"));
        assertTrue(sw.toString().contains(" spec:"));
        assertTrue(sw.toString().contains("-  partitions: 3"));
        assertTrue(sw.toString().contains("-  replicationFactor: 3"));
        assertTrue(sw.toString().contains("+  cleanup.policy: delete"));
        assertTrue(sw.toString().contains("+  replicationFactor: 1"));
    }

    @Test
    void shouldDiffWhenNoLive() {
        kafkactl.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .namespace("namespace")
                .build())
            .spec(Map.of(
                "replicationFactor", 1,
                "partitions", 1,
                "cleanup.policy", "delete"
            ))
            .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
            .thenReturn(Collections.singletonList(resource));
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(apiResourcesService.getResourceDefinitionByKind(any()))
            .thenReturn(Optional.of(apiResource));
        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
            .thenReturn(null);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(HttpResponse
                .ok(resource)
                .header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("--- Topic/prefix.topic-LIVE"));
        assertTrue(sw.toString().contains("+++ Topic/prefix.topic-MERGED"));
        assertTrue(sw.toString().contains("@@ -1,0 +1,14 @@"));
        assertTrue(sw.toString().contains("+---"));
        assertTrue(sw.toString().contains("+apiVersion: v1"));
        assertTrue(sw.toString().contains("+kind: Topic"));
        assertTrue(sw.toString().contains("+metadata:"));
        assertTrue(sw.toString().contains("+  name: prefix.topic"));
        assertTrue(sw.toString().contains("+  namespace: namespace"));
        assertTrue(sw.toString().contains("+spec:"));
        assertTrue(sw.toString().contains("+  partitions: 1"));
        assertTrue(sw.toString().contains("+  replicationFactor: 1"));
    }

    @Test
    void shouldDiffSchemaWhenNoLive() {
        Map<String, Object> specs = new HashMap<>();
        specs.put(SCHEMA_FILE, "src/test/resources/person.avsc");

        kafkactl.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        Resource resource = Resource.builder()
            .kind("Schema")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.schema")
                .namespace("namespace")
                .build())
            .spec(specs)
            .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
            .thenReturn(Collections.singletonList(resource));
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");
        doCallRealMethod().when(resourceService).enrichSchemaContent(any(), any());

        ApiResource apiResource = ApiResource.builder()
            .kind("Schema")
            .namespaced(true)
            .synchronizable(false)
            .path("schemas")
            .names(List.of("schemas", "schema", "sc"))
            .build();

        when(apiResourcesService.getResourceDefinitionByKind(any()))
            .thenReturn(Optional.of(apiResource));
        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
            .thenReturn(null);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(HttpResponse
                .ok(resource)
                .header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(0, code);
        assertFalse(resource.getSpec().get("schema").toString().isBlank());
        assertTrue(sw.toString().contains("--- Schema/prefix.schema-LIVE"));
        assertTrue(sw.toString().contains("+++ Schema/prefix.schema-MERGED"));
        assertTrue(sw.toString().contains("+---"));
        assertTrue(sw.toString().contains("+apiVersion: v1"));
        assertTrue(sw.toString().contains("+kind: Schema"));
        assertTrue(sw.toString().contains("+metadata:"));
        assertTrue(sw.toString().contains("+  cluster: null"));
        assertTrue(sw.toString().contains("+  labels: null"));
        assertTrue(sw.toString().contains("+  name: prefix.schema"));
        assertTrue(sw.toString().contains("+  namespace: namespace"));
        assertTrue(sw.toString().contains("+  schema: "));
        assertTrue(sw.toString().contains("+  schemaFile: src/test/resources/person.avsc"));
    }

    @Test
    void shouldDiffInlineSchema() {
        Map<String, Object> specs = new HashMap<>();
        specs.put("schema", "{schema}");

        kafkactl.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        Resource resource = Resource.builder()
            .kind("Schema")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.schema")
                .namespace("namespace")
                .build())
            .spec(specs)
            .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
            .thenReturn(Collections.singletonList(resource));
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");

        ApiResource apiResource = ApiResource.builder()
            .kind("Schema")
            .namespaced(true)
            .synchronizable(false)
            .path("schemas")
            .names(List.of("schemas", "schema", "sc"))
            .build();

        when(apiResourcesService.getResourceDefinitionByKind(any()))
            .thenReturn(Optional.of(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(HttpResponse
                .ok(resource));

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("--- Schema/prefix.schema-LIVE"));
        assertTrue(sw.toString().contains("+++ Schema/prefix.schema-MERGED"));
        assertTrue(sw.toString().contains("+---"));
        assertTrue(sw.toString().contains("+apiVersion: v1"));
        assertTrue(sw.toString().contains("+kind: Schema"));
        assertTrue(sw.toString().contains("+metadata:"));
        assertTrue(sw.toString().contains("+  cluster: null"));
        assertTrue(sw.toString().contains("+  labels: null"));
        assertTrue(sw.toString().contains("+  name: prefix.schema"));
        assertTrue(sw.toString().contains("+  namespace: namespace"));
        assertTrue(sw.toString().contains("+  schema: '{schema}'"));
    }

    @Test
    void shouldNotDiffSchemaWhenNotExist() {
        Map<String, Object> specs = new HashMap<>();
        specs.put(SCHEMA_FILE, "src/test/resources/not-exist.avsc");

        kafkactl.optionalNamespace = Optional.empty();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        Resource resource = Resource.builder()
            .kind("Schema")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.schema")
                .namespace("namespace")
                .build())
            .spec(specs)
            .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
            .thenReturn(Collections.singletonList(resource));
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");
        doCallRealMethod().when(resourceService).enrichSchemaContent(any(), any());

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains(
            "Cannot open schema file src/test/resources/not-exist.avsc. Schema path must be relative to the CLI."));
    }
}
