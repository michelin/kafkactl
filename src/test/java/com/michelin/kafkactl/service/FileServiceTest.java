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
        assertEquals(
                List.of("config.yml", "ordered-resources.yml", "topic.yml", "unordered-resources.yml"),
                actual.stream().map(File::getName).sorted().toList());
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
        List<Resource> actual = fileService.parseResourceListFromString(
                "{\"apiVersion\": \"v1\", \"kind\": \"Topic\", \"metadata\": {\"name\": \"myTopic\"}}");

        assertEquals(1, actual.size());
        assertEquals("v1", actual.getFirst().getApiVersion());
        assertEquals("Topic", actual.getFirst().getKind());
        assertEquals("myTopic", actual.getFirst().getMetadata().getName());
    }
}
