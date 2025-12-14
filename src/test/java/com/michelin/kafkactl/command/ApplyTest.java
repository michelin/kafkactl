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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
class ApplyTest {
    @Mock
    LoginService loginService;

    @Mock
    FormatService formatService;

    @Mock
    ResourceService resourceService;

    @Mock
    ApiResourcesService apiResourcesService;

    @Mock
    ConfigService configService;

    @Mock
    KafkactlProperties kafkactlProperties;

    @Mock
    Kafkactl kafkactl;

    @InjectMocks
    Apply apply;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(apply);
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
    void shouldNotApplyWhenNotAuthenticated() {
        CommandLine cmd = new CommandLine(apply);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(false);

        int code = cmd.execute();
        assertEquals(1, code);
    }

    @Test
    void shouldNotApplyWhenNoFileInStdin() {
        CommandLine cmd = new CommandLine(apply);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        int code = cmd.execute();
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Required one of -f or stdin."));
    }

    @Test
    void shouldNotApplyWhenFileNotFound() {
        CommandLine cmd = new CommandLine(apply);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        int code = cmd.execute("-f", "src/test/resources/topics/unknown.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("File or directory not found"));
    }

    @Test
    void shouldNotApplyWhenYmlFileNotFound() {
        CommandLine cmd = new CommandLine(apply);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenThrow(new ParameterException(
                        cmd.getCommandSpec().commandLine(), "Could not find YAML or YML files in topic directory."));

        int code = cmd.execute("-f", "src/test/resources/topics-empty");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Could not find YAML or YML files in topic directory."));
    }

    @Test
    void shouldNotApplyWhenInvalidResources() {
        CommandLine cmd = new CommandLine(apply);
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

        int code = cmd.execute("-f", "src/test/resources/topics/topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type(s) Topic."));
    }

    @Test
    void shouldNotApplyWhenNamespaceMismatch() {
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

        CommandLine cmd = new CommandLine(apply);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "src/test/resources/topics/topic.yml", "-n", "namespaceMismatch");
        assertEquals(2, code);
        assertTrue(sw.toString()
                .contains("Namespace mismatch between Kafkactl configuration and YAML resource(s): "
                        + "\"Topic/prefix.topic\"."));
    }

    @Test
    void shouldNotApplyWhenHttpClientResponseException() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(resourceService.parseResources(any(), anyBoolean(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenThrow(exception);

        CommandLine cmd = new CommandLine(apply);

        int code = cmd.execute("-f", "src/test/resources/topics/topic.yml");
        assertEquals(1, code);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldApply() {
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
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(apply);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "src/test/resources/topics/topic.yml");
        assertEquals(0, code);
        verify(resourceService).apply(apiResource, "namespace", resource, false, cmd.getCommandSpec());
    }

    @Test
    void shouldApplyDryRun() {
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
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(apply);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "src/test/resources/topics/topic.yml", "--dry-run");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
        verify(resourceService).apply(apiResource, "namespace", resource, true, cmd.getCommandSpec());
    }

    @Test
    void shouldApplySchema() {
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
        when(resourceService.sortSchemaReferences(any())).thenReturn(List.of());
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(HttpResponse.ok(resource).header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(apply);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "src/test/resources/schemas/schema.yml");
        assertEquals(0, code);
        assertFalse(resource.getSpec().get("schema").toString().isBlank());
        verify(resourceService).apply(apiResource, "namespace", resource, false, cmd.getCommandSpec());
    }

    @Test
    void shouldApplyInlineSchema() {
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
        when(resourceService.sortSchemaReferences(any())).thenReturn(List.of());
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");

        ApiResource apiResource = ApiResource.builder()
                .kind("Schema")
                .namespaced(true)
                .synchronizable(false)
                .path("schemas")
                .names(List.of("schemas", "schema", "sc"))
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean(), any())).thenReturn(HttpResponse.ok(resource));
        doCallRealMethod().when(resourceService).prepareResources(any(), any());

        CommandLine cmd = new CommandLine(apply);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "src/test/resources/topics/topic.yml");
        assertEquals(0, code);
        verify(resourceService).apply(apiResource, "namespace", resource, false, cmd.getCommandSpec());
    }

    @Test
    void shouldNotApplySchemaWhenNotExist() {
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

        CommandLine cmd = new CommandLine(apply);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "src/test/resources/topics/topic.yml");
        assertEquals(2, code);
        assertTrue(
                sw.toString()
                        .contains(
                                "Cannot open schema file src/test/resources/not-exist.avsc. Schema path must be relative to the CLI."));
    }
}
