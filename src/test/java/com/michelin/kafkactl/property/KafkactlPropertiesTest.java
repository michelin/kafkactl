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
package com.michelin.kafkactl.property;

import static com.michelin.kafkactl.property.KafkactlProperties.KAFKACTL_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import com.michelin.kafkactl.service.SystemService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class KafkactlPropertiesTest {
    private final KafkactlProperties kafkactlProperties = new KafkactlProperties();

    @Test
    void shouldGetConfigDirectoryFromUserHome() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG)).thenReturn("");
            mocked.when(() -> SystemService.getProperty(any()))
                    .thenAnswer(answer -> System.getProperty(answer.getArgument(0)));

            String actual = kafkactlProperties.getConfigDirectory();
            assertEquals(System.getProperty("user.home") + "/.kafkactl", actual);
        }
    }

    @Test
    void shouldGetConfigDirectoryFromKafkactlConfig() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG)).thenReturn("src/main/resources/config.yml");

            String actual = kafkactlProperties.getConfigDirectory();
            assertEquals(
                    "src" + System.getProperty("file.separator") + "main" + System.getProperty("file.separator")
                            + "resources",
                    actual);
        }
    }

    @Test
    void shouldGetConfigDirectoryFromKafkactlConfigNoParent() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG)).thenReturn("config.yml");

            String actual = kafkactlProperties.getConfigDirectory();
            assertEquals(".", actual);
        }
    }

    @Test
    void shouldGetConfigDirectoryFromKafkactlConfigParentIsCurrent() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG)).thenReturn("./config.yml");

            String actual = kafkactlProperties.getConfigDirectory();
            assertEquals(".", actual);
        }
    }

    @Test
    void shouldGetConfigPathFromUserHome() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG)).thenReturn("");
            mocked.when(() -> SystemService.getProperty(any()))
                    .thenAnswer(answer -> System.getProperty(answer.getArgument(0)));

            String actual = kafkactlProperties.getConfigPath();
            assertEquals(System.getProperty("user.home") + "/.kafkactl/config.yml", actual);
        }
    }

    @Test
    void shouldGetConfigPathFromKafkactlConfig() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG)).thenReturn("src/main/resources/config.yml");

            String actual = kafkactlProperties.getConfigPath();
            assertEquals("src/main/resources/config.yml", actual);
        }
    }

    @Test
    void shouldGetConfigPathFromKafkactlConfigNoParent() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG)).thenReturn("config.yml");

            String actual = kafkactlProperties.getConfigPath();
            assertEquals("config.yml", actual);
        }
    }

    @Test
    void shouldGetConfigPathFromKafkactlConfigParentIsCurrent() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG)).thenReturn("./config.yml");

            String actual = kafkactlProperties.getConfigPath();
            assertEquals("./config.yml", actual);
        }
    }
}
