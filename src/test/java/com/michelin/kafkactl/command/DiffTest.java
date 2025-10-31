/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.service.ResourceService.SCHEMA_FIELD;
import static com.michelin.kafkactl.service.ResourceService.SCHEMA_FILE_FIELD;
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
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.property.KafkactlProperties;
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
import picocli.CommandLine.ParameterException;

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
    KafkactlProperties kafkactlProperties;

    @Mock
    Kafkactl kafkactl;

    @InjectMocks
    Diff diff;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(false);

        int code = cmd.execute();
        assertEquals(1, code);
        assertTrue(sw.toString()
                .contains("No valid current context found. "
                        + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotDiffWhenNotAuthenticated() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(false);

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(1, code);
    }

    @Test
    void shouldNotDiffWhenNoFileInStdin() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenThrow(new ParameterException(
                        cmd.getCommandSpec().commandLine(), "Could not find YAML or YML files in topic directory."));

        int code = cmd.execute("-f", "topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Could not find YAML or YML files in topic directory."));
    }

    @Test
    void shouldNotDiffWhenInvalidResources() {
        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("prefix.topic").build())
                .spec(Collections.emptyMap())
                .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        doThrow(new ParameterException(
                        cmd.getCommandSpec().commandLine(), "The server does not have resource type(s) Topic."))
                .when(resourceService)
                .validateAllowedResources(any(), any());

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type(s) Topic."));
    }

    @Test
    void shouldNotDiffWhenNamespaceMismatch() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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

        int code = cmd.execute("-f", "topic.yml", "-n", "namespaceMismatch");
        assertEquals(2, code);
        assertTrue(sw.toString()
                .contains("Namespace mismatch between Kafkactl configuration and YAML resource(s): "
                        + "\"Topic/prefix.topic\"."));
    }

    @Test
    void shouldNotDiffWhenHttpClientResponseException() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenThrow(exception);

        CommandLine cmd = new CommandLine(diff);

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(1, code);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldNotDiffWhenHttpClientResponseExceptionDuringApply() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Map.of(
                        "replicationFactor", 3,
                        "partitions", 3))
                .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any())).thenReturn(null);

        CommandLine cmd = new CommandLine(diff);

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(1, code);
    }

    @Test
    void shouldNotDiffApplyHasNoBody() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Map.of(
                        "replicationFactor", 3,
                        "partitions", 3))
                .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any())).thenReturn(HttpResponse.ok());

        CommandLine cmd = new CommandLine(diff);

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(1, code);
    }

    @Test
    void shouldDiff() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
                        "cleanup.policy", "delete"))
                .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Map.of(
                        "replicationFactor", 3,
                        "partitions", 3))
                .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

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
    void shouldDiffWithIgnoreFields() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
                        "cleanup.policy", "delete"))
                .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Map.of(
                        "replicationFactor", 3,
                        "partitions", 3))
                .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        // Ignore replicationFactor field
        int code = cmd.execute("-f", "topic.yml", "--ignore-fields", "spec.replicationFactor");
        assertEquals(0, code);

        String output = sw.toString();
        assertTrue(output.contains("--- Topic/prefix.topic-LIVE"));
        assertTrue(output.contains("+++ Topic/prefix.topic-MERGED"));
        assertTrue(output.contains("   name: prefix.topic"));
        assertTrue(output.contains("   namespace: namespace"));
        assertTrue(output.contains(" spec:"));
        assertTrue(output.contains("-  partitions: 3"));
        assertTrue(output.contains("+  cleanup.policy: delete"));
        assertTrue(output.contains("+  partitions: 1"));

        // replicationFactor should NOT appear in the diff since it's ignored
        assertFalse(output.contains("replicationFactor"));
    }

    @Test
    void shouldDiffWithMultipleIgnoreFields() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
                        "cleanup.policy", "delete"))
                .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Map.of(
                        "replicationFactor", 3,
                        "partitions", 3))
                .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        // Ignore both replicationFactor and partitions fields
        int code = cmd.execute("-f", "topic.yml", "--ignore-fields", "spec.replicationFactor,spec.partitions");
        assertEquals(0, code);

        String output = sw.toString();
        assertTrue(output.contains("--- Topic/prefix.topic-LIVE"));
        assertTrue(output.contains("+++ Topic/prefix.topic-MERGED"));
        assertTrue(output.contains("   name: prefix.topic"));
        assertTrue(output.contains("   namespace: namespace"));
        assertTrue(output.contains("spec:"));
        assertTrue(output.contains("+  cleanup.policy: delete"));

        // Both replicationFactor and partitions should NOT appear in the diff
        assertFalse(output.contains("replicationFactor"));
        assertFalse(output.contains("partitions"));
    }

    @Test
    void shouldDiffWithIgnoreFieldWithSubFields() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .labels(Map.of("key", "value", "key2", "value2", "key3", "value3", "key4", "value4"))
                        .build())
                .spec(Map.of(
                        "replicationFactor", 1,
                        "partitions", 1,
                        "cleanup.policy", "delete"))
                .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .labels(Map.of("world", "hello", "key2", "value2", "key3", "value3", "key4", "value4"))
                        .build())
                .spec(Map.of(
                        "replicationFactor", 3,
                        "partitions", 3))
                .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        // Ignore both metadata.labels object
        int code = cmd.execute("-f", "topic.yml", "--ignore-fields", "metadata.labels");
        assertEquals(0, code);

        String output = sw.toString();
        assertTrue(output.contains("--- Topic/prefix.topic-LIVE"));
        assertTrue(output.contains("+++ Topic/prefix.topic-MERGED"));
        assertTrue(output.contains("   name: prefix.topic"));
        assertTrue(output.contains("   namespace: namespace"));
        assertTrue(output.contains("spec:"));
        assertTrue(output.contains("+  cleanup.policy: delete"));

        // As labels are ignored no object should be seen in the diff
        assertFalse(output.contains("labels"));
    }

    @Test
    void shouldDiffWhenNoLive() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
                        "cleanup.policy", "delete"))
                .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(null);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

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
        specs.put(SCHEMA_FILE_FIELD, "src/test/resources/person.avsc");

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Schema")
                .namespaced(true)
                .synchronizable(false)
                .path("schemas")
                .names(List.of("schemas", "schema", "sc"))
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.sortSchemaReferences(any())).thenReturn(List.of("com.michelin.kafkactl.PersonAvro"));
        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(null);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

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
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        Resource resource = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.schema")
                        .namespace("namespace")
                        .build())
                .spec(
                        Map.of(
                                SCHEMA_FIELD,
                                "{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"com.michelin.kafka.avro\", \"fields\": [{ \"name\": \"ref\", \"type\": \"string\" }]}"))
                .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Schema")
                .namespaced(true)
                .synchronizable(false)
                .path("schemas")
                .names(List.of("schemas", "schema", "sc"))
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.sortSchemaReferences(any())).thenReturn(List.of("com.michelin.kafka.avro.Customer"));
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any())).thenReturn(HttpResponse.ok(resource));

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
        assertTrue(sw.toString().contains("+spec:"));
        assertTrue(
                sw.toString()
                        .contains(
                                "+  schema: '{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"com.michelin.kafka.avro\","));
        assertTrue(sw.toString().contains("+    \"fields\": [{ \"name\": \"ref\", \"type\": \"string\" }]}'"));
    }

    @Test
    void shouldNotDiffSchemaWhenNotExist() {
        Map<String, Object> specs = new HashMap<>();
        specs.put(SCHEMA_FILE_FIELD, "src/test/resources/not-exist.avsc");

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(
                sw.toString()
                        .contains(
                                "Cannot open schema file src/test/resources/not-exist.avsc. Schema path must be relative to the CLI."));
    }

    @Test
    void shouldDiffWithIgnoreFlatFormatField() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
                        "cleanup.policy", "delete"))
                .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Map.of(
                        "replicationFactor", 3,
                        "partitions", 3,
                        "cleanup.policy", "compact"))
                .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        // Ignore cleanup.policy field (flat format)
        int code = cmd.execute("-f", "topic.yml", "--ignore-fields", "spec.cleanup.policy");
        assertEquals(0, code);

        String output = sw.toString();
        assertTrue(output.contains("--- Topic/prefix.topic-LIVE"));
        assertTrue(output.contains("+++ Topic/prefix.topic-MERGED"));
        assertTrue(output.contains("   name: prefix.topic"));
        assertTrue(output.contains("   namespace: namespace"));
        assertTrue(output.contains("spec:"));
        assertTrue(output.contains("-  partitions: 3"));
        assertTrue(output.contains("-  replicationFactor: 3"));
        assertTrue(output.contains("+  partitions: 1"));
        assertTrue(output.contains("+  replicationFactor: 1"));

        // cleanup.policy should NOT appear in the diff since it's ignored
        assertFalse(output.contains("cleanup.policy"));
    }

    @Test
    void shouldDiffWithIgnoreNonExistentField() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Map.of(
                        "replicationFactor", 1,
                        "partitions", 1))
                .build();

        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));

        Resource live = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Map.of(
                        "replicationFactor", 3,
                        "partitions", 3))
                .build();

        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(live);
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(diff);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        // Ignore non-existent fields - should not break anything
        int code = cmd.execute(
                "-f", "topic.yml", "--ignore-fields", "spec.nonexistent.field,spec.another.missing,metadata.fake.path");
        assertEquals(0, code);

        String output = sw.toString();
        assertTrue(output.contains("--- Topic/prefix.topic-LIVE"));
        assertTrue(output.contains("+++ Topic/prefix.topic-MERGED"));
        assertTrue(output.contains("   name: prefix.topic"));
        assertTrue(output.contains("   namespace: namespace"));
        assertTrue(output.contains("spec:"));
        assertTrue(output.contains("-  partitions: 3"));
        assertTrue(output.contains("-  replicationFactor: 3"));
        assertTrue(output.contains("+  partitions: 1"));
        assertTrue(output.contains("+  replicationFactor: 1"));
    }
}
