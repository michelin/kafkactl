package com.michelin.kafkactl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.michelin.kafkactl.model.Resource;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @InjectMocks
    FileService fileService;

    @Test
    void shouldComputeYamlFileListRecursive() {
        List<File> actual = fileService.computeYamlFileList(new File("src/test/resources"), true);
        assertEquals("config.yml", actual.get(0).getName());
        assertEquals("topic.yml", actual.get(1).getName());
        assertEquals(2, actual.size());
    }

    @Test
    void shouldComputeYamlFileListNonRecursive() {
        List<File> actual = fileService.computeYamlFileList(new File("src/test/resources"), false);
        assertEquals("config.yml", actual.getFirst().getName());
        assertEquals(1, actual.size());
    }

    @Test
    void shouldComputeYamlFileListFile() {
        List<File> actual = fileService.computeYamlFileList(new File("src/test/resources/topics/topic.yml"), false);
        assertEquals("topic.yml", actual.getFirst().getName());
        assertEquals(1, actual.size());
    }

    @Test
    void shouldParseResourceListFromFiles() {
        List<Resource> actual = fileService.parseResourceListFromFiles(
            Collections.singletonList(new File("src/test/resources/topics/topic.yml")));
        assertEquals("Topic", actual.getFirst().getKind());
        assertEquals("myPrefix.topic", actual.getFirst().getMetadata().getName());
        assertEquals(3, actual.getFirst().getSpec().get("replicationFactor"));
        assertEquals(1, actual.size());
    }

    @Test
    void shouldParseResourceListFromString() {
        List<Resource> actual = fileService
            .parseResourceListFromString(
                "{\"apiVersion\": \"v1\", \"kind\": \"Topic\", \"metadata\": {\"name\": \"myTopic\"}}");

        assertEquals(1, actual.size());
        assertEquals("v1", actual.getFirst().getApiVersion());
        assertEquals("Topic", actual.getFirst().getKind());
        assertEquals("myTopic", actual.getFirst().getMetadata().getName());
    }
}
