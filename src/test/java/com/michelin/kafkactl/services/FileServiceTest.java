package com.michelin.kafkactl.services;

import com.michelin.kafkactl.models.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @InjectMocks
    public FileService fileService;

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
        assertEquals("config.yml", actual.get(0).getName());
        assertEquals(1, actual.size());
    }

    @Test
    void shouldComputeYamlFileListFile() {
        List<File> actual = fileService.computeYamlFileList(new File("src/test/resources/topics/topic.yml"), false);
        assertEquals("topic.yml", actual.get(0).getName());
        assertEquals(1, actual.size());
    }

    @Test
    void shouldParseResourceListFromFiles() {
        List<Resource> actual = fileService.parseResourceListFromFiles(Collections.singletonList(new File("src/test/resources/topics/topic.yml")));
        assertEquals("Topic", actual.get(0).getKind());
        assertEquals("myPrefix.topic", actual.get(0).getMetadata().getName());
        assertEquals(3, actual.get(0).getSpec().get("replicationFactor"));
        assertEquals(1, actual.size());
    }

    @Test
    void shouldParseResourceListFromString() {
        List<Resource> actual = fileService
                .parseResourceListFromString("{\"apiVersion\": \"v1\", \"kind\": \"Topic\", \"metadata\": {\"name\": \"myTopic\"}}");

        assertEquals(1, actual.size());
        assertEquals("v1", actual.get(0).getApiVersion());
        assertEquals("Topic", actual.get(0).getKind());
        assertEquals("myTopic", actual.get(0).getMetadata().getName());
    }
}
