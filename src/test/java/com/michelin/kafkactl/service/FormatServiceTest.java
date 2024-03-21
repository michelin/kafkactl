package com.michelin.kafkactl.service;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.service.FormatService.YAML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.michelin.kafkactl.command.KafkactlCommand;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@MicronautTest
class FormatServiceTest {
    @Inject
    public FormatService formatService;

    @Test
    void shouldDisplayListTable() {
        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of("configs",
                Map.of("retention.ms", "60000",
                    "cleanup.policy", "delete"
                )))
            .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayList("Topic", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("TOPIC         RETENTION  POLICY  AGE"));
        assertTrue(sw.toString().contains("prefix.topic  1m         delete"));
    }

    @Test
    void shouldDisplayListTableEmptySpecs() {
        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .build())
            .spec(Collections.emptyMap())
            .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
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
            .spec(Map.of("configs",
                Map.of("retention.ms", "60000",
                    "cleanup.policy", "delete"
                )))
            .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayList("Topic", Collections.singletonList(resource), YAML, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("  name: prefix.topic"));
        assertTrue(sw.toString().contains("    retention.ms: '60000'"));
        assertTrue(sw.toString().contains("    cleanup.policy: delete"));
    }

    @Test
    void shouldDisplayListYamlEmptySpecs() {
        Resource resource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .build())
            .spec(Collections.emptyMap())
            .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
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
            .metadata(Metadata.builder()
                .name("roleBinding")
                .build())
            .spec(Map.of(
                "subject", Map.of("subjectName", "GROUP"),
                "role",
                Map.of(
                    "resourceTypes", List.of("topics", "acls", "connectors"),
                    "verbs", List.of("GET", "POST", "PUT", "DELETE"))
            ))
            .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displayList("RoleBinding", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("ROLE_BINDING  GROUP  VERBS                RESOURCES"));
        assertTrue(sw.toString().contains("roleBinding   GROUP  GET,POST,PUT,DELETE  topics,acls,connectors"));
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
            .spec(Map.of("configs",
                Map.of("retention.ms", "60000",
                    "cleanup.policy", "delete"
                )))
            .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        formatService.displaySingle(resource, TABLE, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("TOPIC         RETENTION  POLICY  AGE"));
        assertTrue(sw.toString().contains("prefix.topic  1m         delete"));
    }

    @Test
    void shouldDisplayErrorWithNameKindAndDetails() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse
            .serverError(Status.builder()
                .details(Status.StatusDetails.builder()
                    .causes(List.of("Error 1", "Error 2", "Error 3", "Error 4", "Error 5"))
                    .build())
                .message("An error occurred")
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
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse
            .serverError(Status.builder()
                .message("An error occurred")
                .build()));

        formatService.displayError(exception, "Topic", "myTopic", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic \"myTopic\" failed because error (500)."));
    }

    @Test
    void shouldDisplayErrorWithNameKindButNoStatus() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse
            .serverError());

        formatService.displayError(exception, "Topic", "myTopic", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic \"myTopic\" failed because error (500)."));
    }

    @Test
    void shouldDisplayErrorWithNameAndDetails() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse
            .serverError(Status.builder()
                .details(Status.StatusDetails.builder()
                    .causes(List.of("Error 1", "Error 2", "Error 3", "Error 4", "Error 5"))
                    .build())
                .message("An error occurred")
                .build()));

        formatService.displayError(exception, "Topic", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic failed because an error occurred (500):"));
        assertTrue(sw.toString().contains(" - Error 1"));
        assertTrue(sw.toString().contains(" - Error 2"));
        assertTrue(sw.toString().contains(" - Error 3"));
        assertTrue(sw.toString().contains(" - Error 4"));
        assertTrue(sw.toString().contains(" - Error 5"));
    }

    @Test
    void shouldDisplayErrorWithNameButNoDetails() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse
            .serverError(Status.builder()
                .message("An error occurred")
                .build()));

        formatService.displayError(exception, "Topic", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic failed because error (500)."));
    }

    @Test
    void shouldDisplayErrorWithNameButNoStatus() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse
            .serverError());

        formatService.displayError(exception, "Topic", cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Topic failed because error (500)."));
    }

    @Test
    void shouldDisplayErrorWithDetails() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse
            .serverError(Status.builder()
                .details(Status.StatusDetails.builder()
                    .causes(List.of("Error 1", "Error 2", "Error 3", "Error 4", "Error 5"))
                    .build())
                .message("An error occurred")
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
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse
            .serverError(Status.builder()
                .message("An error occurred")
                .build()));

        formatService.displayError(exception, cmd.getCommandSpec());

        assertTrue(sw.toString().contains("Failed because error (500)."));
    }

    @Test
    void shouldDisplayErrorWithNoStatus() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse
            .serverError());

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
