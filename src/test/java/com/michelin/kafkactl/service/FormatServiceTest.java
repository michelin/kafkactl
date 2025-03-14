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
package com.michelin.kafkactl.service;

import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.model.Output.YAML;
import static com.michelin.kafkactl.model.Output.YML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.model.Status;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@MicronautTest
class FormatServiceTest {
    @Inject
    FormatService formatService;

    @Test
    void shouldDisplayListTable() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of("configs", Map.of("retention.ms", "60000", "cleanup.policy", "delete")))
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayList("Topic", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("TOPIC         RETENTION  POLICY  AGE"));
        assertTrue(sw.toString().contains("prefix.topic  1m         delete"));
    }

    @Test
    void shouldDisplayEmptyInsteadOfNull() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name(null)
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of("configs", Map.of("retention.ms", "60000", "cleanup.policy", "delete")))
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayList("Topic", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("TOPIC  RETENTION  POLICY  AGE"));
        assertTrue(sw.toString().contains("       1m         delete"));
    }

    @Test
    void shouldDisplayListTableEmptySpecs() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("prefix.topic").build())
                .spec(Collections.emptyMap())
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayList("Topic", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("TOPIC         RETENTION  POLICY  AGE"));
        assertTrue(sw.toString().contains("prefix.topic"));
    }

    @Test
    void shouldDisplayListYaml() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of("configs", Map.of("retention.ms", "60000", "cleanup.policy", "delete")))
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayList("Topic", Collections.singletonList(resource), YML, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("  name: prefix.topic"));
        assertTrue(sw.toString().contains("    retention.ms: '60000'"));
        assertTrue(sw.toString().contains("    cleanup.policy: delete"));
    }

    @Test
    void shouldDisplayListYamlEmptySpecs() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("prefix.topic").build())
                .spec(Collections.emptyMap())
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayList("Topic", Collections.singletonList(resource), YAML, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("  name: prefix.topic"));
    }

    @Test
    void shouldDisplayResourceWithAnArrayCell() {
        Resource resource = Resource.builder()
                .kind("RoleBinding")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("roleBinding").build())
                .spec(Map.of(
                        "subject",
                        Map.of("subjectName", "GROUP"),
                        "role",
                        Map.of(
                                "resourceTypes", List.of("topics", "acls", "connectors"),
                                "verbs", List.of("GET", "POST", "PUT", "DELETE"))))
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayList("RoleBinding", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());

        assertTrue(Pattern.compile("ROLE_BINDING\\s+GROUP\\s+VERBS\\s+RESOURCES")
                .matcher(sw.toString())
                .find());
        assertTrue(Pattern.compile("roleBinding\\s+GROUP\\s+GET,POST,PUT,DELETE\\s+topics,acls,connectors")
                .matcher(sw.toString())
                .find());
    }

    @Test
    void shouldDisplayNoResource() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayNoResource(List.of(apiResource), null, "*", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("No topic to display"));
    }

    @Test
    void shouldDisplayNoResourceMatchingName() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayNoResource(List.of(apiResource), Map.of(), "myTopic", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("No topic matches name \"myTopic\""));
    }

    @Test
    void shouldDisplayNoResourceMatchingSearch() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayNoResource(List.of(apiResource), Map.of("param", "value"), "*", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("No topic matches search \"param=value\""));
    }

    @Test
    void shouldDisplayNoResourceMatchingNameAndSearch() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayNoResource(List.of(apiResource), Map.of("param", "value"), "test", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("No topic matches name \"test\" and search \"param=value\""));
    }

    @Test
    void shouldDisplayNoResourceMatchingNameAndMultipleSearch() {
        final ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        Map<String, String> search = new HashMap<>();
        search.put("key1", "value1");
        search.put("key2", "value2");

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayNoResource(List.of(apiResource), search, "test", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("No topic matches name \"test\" and search \"key1=value1,key2=value2\""));
    }

    @Test
    void shouldDisplaySingle() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of("configs", Map.of("retention.ms", "60000", "cleanup.policy", "delete")))
                .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displaySingle(resource, TABLE, cmd.getCommandSpec());

        assertTrue(Pattern.compile("TOPIC\\s+RETENTION\\s+POLICY\\s+AGE")
                .matcher(sw.toString())
                .find());
        assertTrue(Pattern.compile("prefix.topic\\s+1m\\s+delete")
                .matcher(sw.toString())
                .find());
    }

    @Test
    void shouldDisplayErrorWithNameKindAndDetails() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException(
                "error",
                HttpResponse.serverError(Status.builder()
                        .details(Status.StatusDetails.builder()
                                .causes(List.of("Error 1", "Error 2", "Error 3", "Error 4", "Error 5"))
                                .build())
                        .message("An error occurred")
                        .reason("")
                        .code(500)
                        .build()));

        formatService.displayError(exception, "Topic", "myTopic", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic \"myTopic\" failed because an error occurred (500):"));
        assertTrue(sw.toString().contains(" - Error 1"));
        assertTrue(sw.toString().contains(" - Error 2"));
        assertTrue(sw.toString().contains(" - Error 3"));
        assertTrue(sw.toString().contains(" - Error 4"));
        assertTrue(sw.toString().contains(" - Error 5"));
    }

    @Test
    void shouldDisplayErrorWithNameKindButNoDetails() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException(
                "error",
                HttpResponse.serverError(Status.builder()
                        .message("An error occurred")
                        .reason("")
                        .code(500)
                        .build()));

        formatService.displayError(exception, "Topic", "myTopic", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic \"myTopic\" failed because error (500)."));
    }

    @Test
    void shouldDisplayErrorWithNameKindButNoStatus() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());

        formatService.displayError(exception, "Topic", "myTopic", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic \"myTopic\" failed because error (500)."));
    }

    @Test
    void shouldDisplayErrorWithNameAndDetails() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException(
                "error",
                HttpResponse.serverError(Status.builder()
                        .details(Status.StatusDetails.builder()
                                .causes(List.of("Error 1", "Error 2", "Error 3", "Error 4", "Error 5"))
                                .build())
                        .message("An error occurred")
                        .reason("")
                        .code(500)
                        .build()));

        formatService.displayError(exception, "Topic", "*", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic failed because an error occurred (500):"));
        assertTrue(sw.toString().contains(" - Error 1"));
        assertTrue(sw.toString().contains(" - Error 2"));
        assertTrue(sw.toString().contains(" - Error 3"));
        assertTrue(sw.toString().contains(" - Error 4"));
        assertTrue(sw.toString().contains(" - Error 5"));
    }

    @Test
    void shouldDisplayErrorWithNameButNoDetails() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException(
                "error",
                HttpResponse.serverError(Status.builder()
                        .message("An error occurred")
                        .reason("")
                        .code(500)
                        .build()));

        formatService.displayError(exception, "Topic", "*", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic failed because error (500)."));
    }

    @Test
    void shouldDisplayErrorWithNameButNoStatus() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());

        formatService.displayError(exception, "Topic", "*", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic failed because error (500)."));
    }

    @Test
    void shouldDisplayErrorWithDetails() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException(
                "error",
                HttpResponse.serverError(Status.builder()
                        .details(Status.StatusDetails.builder()
                                .causes(List.of("Error 1", "Error 2", "Error 3", "Error 4", "Error 5"))
                                .build())
                        .message("An error occurred")
                        .reason("")
                        .code(500)
                        .build()));

        formatService.displayError(exception, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Failed because an error occurred (500):"));
        assertTrue(sw.toString().contains(" - Error 1"));
        assertTrue(sw.toString().contains(" - Error 2"));
        assertTrue(sw.toString().contains(" - Error 3"));
        assertTrue(sw.toString().contains(" - Error 4"));
        assertTrue(sw.toString().contains(" - Error 5"));
    }

    @Test
    void shouldDisplayErrorWithNoDetails() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException(
                "error",
                HttpResponse.serverError(Status.builder()
                        .message("An error occurred")
                        .reason("")
                        .code(500)
                        .build()));

        formatService.displayError(exception, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Failed because error (500)."));
    }

    @Test
    void shouldDisplayErrorWithNoStatus() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());

        formatService.displayError(exception, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Failed because error (500)."));
    }

    @Test
    void shouldPrettifyKind() {
        assertEquals("Topic", formatService.prettifyKind("topic"));
        assertEquals("Change connector state", formatService.prettifyKind("changeConnectorState"));
        assertEquals("Change connector state", formatService.prettifyKind("ChangeConnectorState"));
    }
}
