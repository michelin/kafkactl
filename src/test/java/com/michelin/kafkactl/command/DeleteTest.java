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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.property.KafkactlProperties;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.FileService;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
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
class DeleteTest {
    @Mock
    FormatService formatService;

    @Mock
    KafkactlProperties kafkactlProperties;

    @Mock
    ResourceService resourceService;

    @Mock
    LoginService loginService;

    @Mock
    ApiResourcesService apiResourcesService;

    @Mock
    FileService fileService;

    @Mock
    ConfigService configService;

    @InjectMocks
    Delete delete;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(false);

        int code = cmd.execute("topic", "myTopic");
        assertEquals(1, code);
        assertTrue(sw.toString()
                .contains("No valid current context found. "
                        + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotDeleteByNameWhenNotAuthenticated() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(false);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic", "myTopic");
        assertEquals(1, code);
    }

    @Test
    void shouldNotDeleteByFileWhenYmlFileNotFound() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        when(fileService.computeYamlFileList(any(), anyBoolean())).thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Could not find YAML or YML files in topic directory."));
    }

    @Test
    void shouldNotDeleteByFileWhenInvalidResources() {
        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        when(fileService.parseResourceListFromFiles(any())).thenReturn(Collections.singletonList(resource));
        doThrow(new ParameterException(
                        cmd.getCommandSpec().commandLine(), "The server does not have resource type(s) Topic."))
                .when(resourceService)
                .validateAllowedResources(any(), any());

        int code = cmd.execute("-f", "topic", "-n", "namespace");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type(s) Topic."));
    }

    @Test
    void shouldNotDeleteByNameWhenInvalidResources() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByName(any())).thenReturn(Optional.empty());

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topic", "myTopic", "-n", "namespace");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type(s) topic."));
    }

    @Test
    void shouldNotDeleteByFileWhenNamespaceMismatch() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        when(fileService.parseResourceListFromFiles(any())).thenReturn(Collections.singletonList(resource));

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic", "-n", "namespaceMismatch");
        assertEquals(2, code);
        assertTrue(sw.toString()
                .contains("Namespace mismatch between Kafkactl configuration and YAML resource(s): "
                        + "\"Topic/prefix.topic\"."));
    }

    @Test
    void shouldDeleteByFile() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        when(fileService.parseResourceListFromFiles(any())).thenReturn(Collections.singletonList(resource));

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic", "-n", "namespace");
        assertEquals(0, code);
    }

    @Test
    void shouldDeleteOneVersionByFile() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Map.of("version", "1"))
                .build();

        when(fileService.parseResourceListFromFiles(any())).thenReturn(Collections.singletonList(resource));

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic", "-n", "namespace");
        assertEquals(0, code);
    }

    @Test
    void shouldDeleteByName() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByName(any())).thenReturn(Optional.of(apiResource));
        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topic", "prefix.topic", "-n", "namespace");
        assertEquals(0, code);
    }

    @Test
    void shouldDeleteOneVersionByName() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByName(any())).thenReturn(Optional.of(apiResource));
        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topic", "prefix.topic", "-n", "namespace", "-V", "latest");
        assertEquals(0, code);
    }

    @Test
    void shouldNotDeleteByFileWhenInDryRunMode() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("prefix.topic").build())
                .spec(Collections.emptyMap())
                .build();

        when(fileService.parseResourceListFromFiles(any())).thenReturn(Collections.singletonList(resource));

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic", "--dry-run", "-n", "namespace");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
    }

    @Test
    void shouldNotDeleteByFileWhenFail() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        when(fileService.parseResourceListFromFiles(any())).thenReturn(Collections.singletonList(resource));

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(false);

        CommandLine cmd = new CommandLine(delete);

        int code = cmd.execute("-f", "topic", "-n", "namespace");
        assertEquals(1, code);
    }

    @Test
    void shouldNotDeleteByFileWhenHttpClientResponseException() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        when(fileService.parseResourceListFromFiles(any())).thenReturn(Collections.singletonList(resource));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());

        when(apiResourcesService.getResourceDefinitionByKind(any())).thenThrow(exception);

        CommandLine cmd = new CommandLine(delete);

        int code = cmd.execute("-f", "topic", "-n", "namespace");
        assertEquals(1, code);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldDeleteByNameWithWildcard() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByName(any())).thenReturn(Optional.of(apiResource));
        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topic", "*", "-n", "namespace", "--execute");
        assertEquals(0, code);
    }

    @Test
    void shouldNotDeleteByNameWithWildcardWithoutExecute() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topics", "*", "-n", "namespace");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("You are about to potentially delete multiple resources with wildcard"));
        assertTrue(sw.toString()
                .contains("Rerun the command with option --dry-run to see the resources that will be deleted."));
        assertTrue(sw.toString().contains("Rerun the command with option --execute to execute this operation."));
        verify(resourceService, never()).delete(any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void shouldNotDeleteByNameWithQuestionMarkWildcardWithoutExecute() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topics", "prefix.topic?", "-n", "namespace");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("You are about to potentially delete multiple resources with wildcard"));
        assertTrue(sw.toString()
                .contains("Rerun the command with option --dry-run to see the resources that will be deleted."));
        assertTrue(sw.toString().contains("Rerun the command with option --execute to execute this operation."));
        verify(resourceService, never()).delete(any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void shouldNotDeleteByNameWithWildcardInDryRunMode() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByName(any())).thenReturn(Optional.of(apiResource));
        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topics", "*", "--dry-run", "-n", "namespace");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
    }

    @Test
    void shouldNotDeleteByNameWithWildcardWithExecuteInDryRunMode() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(apiResourcesService.getResourceDefinitionByName(any())).thenReturn(Optional.of(apiResource));
        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.delete(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(true);

        CommandLine cmd = new CommandLine(delete);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topics", "prefix.topi?", "--execute", "--dry-run", "-n", "namespace");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Dry run execution."));
        assertFalse(sw.toString().contains("You are about to potentially delete multiple resources with wildcard"));
    }
}
