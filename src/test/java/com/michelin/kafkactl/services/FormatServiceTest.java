package com.michelin.kafkactl.services;

import com.michelin.kafkactl.KafkactlCommand;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class FormatServiceTest {
    @Inject
    public FormatService formatService;

    @Test
    void shouldDisplayListTable() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
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
                .metadata(ObjectMeta.builder()
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
}
