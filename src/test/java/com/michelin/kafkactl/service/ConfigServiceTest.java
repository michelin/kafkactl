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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.property.KafkactlProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {
    @Mock
    KafkactlProperties kafkactlProperties;

    @Mock
    LoginService loginService;

    @InjectMocks
    ConfigService configService;

    @Test
    void shouldGetCurrentContextName() {
        KafkactlProperties.ContextsProperties contextPropertiesOne = KafkactlProperties.ContextsProperties.builder()
                .name("name")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("userToken")
                        .api("https://ns4kafka.com")
                        .namespace("namespace")
                        .build())
                .build();

        KafkactlProperties.ContextsProperties contextPropertiesTwo = KafkactlProperties.ContextsProperties.builder()
                .name("name2")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("userToken2")
                        .api("https://ns4kafka.com")
                        .namespace("namespace2")
                        .build())
                .build();

        when(kafkactlProperties.getContexts()).thenReturn(List.of(contextPropertiesOne, contextPropertiesTwo));
        when(kafkactlProperties.getApi()).thenReturn("https://ns4kafka.com");
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        when(kafkactlProperties.getUserToken()).thenReturn("userToken");

        String actual = configService.getCurrentContextName();

        assertEquals("name", actual);
    }

    @Test
    void shouldNotGetCurrentContextName() {
        KafkactlProperties.ContextsProperties contextPropertiesOne = KafkactlProperties.ContextsProperties.builder()
                .name("name")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("userToken")
                        .api("https://ns4kafka.com")
                        .namespace("namespace")
                        .build())
                .build();

        KafkactlProperties.ContextsProperties contextPropertiesTwo = KafkactlProperties.ContextsProperties.builder()
                .name("name2")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("userToken2")
                        .api("https://ns4kafka.com")
                        .namespace("namespace2")
                        .build())
                .build();

        when(kafkactlProperties.getContexts()).thenReturn(List.of(contextPropertiesOne, contextPropertiesTwo));
        when(kafkactlProperties.getApi()).thenReturn("https://ns4kafka.com");
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("badNamespace");

        String actual = configService.getCurrentContextName();

        assertNull(actual);
    }

    @Test
    void shouldGetContextByName() {
        KafkactlProperties.ContextsProperties contextPropertiesOne = KafkactlProperties.ContextsProperties.builder()
                .name("name")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("userToken")
                        .api("https://ns4kafka.com")
                        .namespace("namespace")
                        .build())
                .build();

        KafkactlProperties.ContextsProperties contextPropertiesTwo = KafkactlProperties.ContextsProperties.builder()
                .name("name2")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("userToken2")
                        .api("https://ns4kafka.com")
                        .namespace("namespace2")
                        .build())
                .build();

        when(kafkactlProperties.getContexts()).thenReturn(List.of(contextPropertiesOne, contextPropertiesTwo));

        Optional<KafkactlProperties.ContextsProperties> actual = configService.getContextByName("name");

        assertTrue(actual.isPresent());
        assertEquals(contextPropertiesOne, actual.get());
    }

    @Test
    void shouldNotGetContextByName() {
        KafkactlProperties.ContextsProperties contextPropertiesOne = KafkactlProperties.ContextsProperties.builder()
                .name("name")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("userToken")
                        .api("https://ns4kafka.com")
                        .namespace("namespace")
                        .build())
                .build();

        KafkactlProperties.ContextsProperties contextPropertiesTwo = KafkactlProperties.ContextsProperties.builder()
                .name("name2")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("userToken2")
                        .api("https://ns4kafka.com")
                        .namespace("namespace2")
                        .build())
                .build();

        when(kafkactlProperties.getContexts()).thenReturn(List.of(contextPropertiesOne, contextPropertiesTwo));

        Optional<KafkactlProperties.ContextsProperties> actual = configService.getContextByName("notFound");

        assertFalse(actual.isPresent());
    }

    @Test
    void shouldUpdateConfigurationContext(@TempDir Path tempDir) throws IOException {
        Path configPath = Files.copy(Path.of("src/test/resources/application-test.yml"), tempDir.resolve("config.yml"));
        when(kafkactlProperties.getConfigPath()).thenReturn(configPath.toString());

        KafkactlProperties.ContextsProperties contextPropertiesToSet = KafkactlProperties.ContextsProperties.builder()
                .name("name")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("userToken")
                        .api("https://ns4kafka.com")
                        .namespace("namespace")
                        .build())
                .build();

        configService.updateConfigurationContext(contextPropertiesToSet);

        String actual = Files.readString(configPath);

        assertTrue(actual.contains("  current-namespace: namespace"));
        assertTrue(actual.contains("  api: https://ns4kafka.com"));
        assertTrue(actual.contains("  user-token: userToken"));
        verify(loginService).deleteJwtFile();
    }

    @Test
    void shouldUpdateConfigurationContextWithoutNamespace(@TempDir Path tempDir) throws IOException {
        Path configPath = Files.copy(Path.of("src/test/resources/application-test.yml"), tempDir.resolve("config.yml"));
        when(kafkactlProperties.getConfigPath()).thenReturn(configPath.toString());

        KafkactlProperties.ContextsProperties contextPropertiesToSet = KafkactlProperties.ContextsProperties.builder()
                .name("name")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("userToken")
                        .api("https://ns4kafka.com")
                        .build())
                .build();

        configService.updateConfigurationContext(contextPropertiesToSet);

        String actual = Files.readString(configPath);

        assertFalse(actual.contains("current-namespace"));
        assertTrue(actual.contains("  api: https://ns4kafka.com"));
        assertTrue(actual.contains("  user-token: userToken"));
    }

    @Test
    void shouldNotGetMissingProperty() {
        when(kafkactlProperties.getApi()).thenReturn("https://ns4kafka.com");
        when(kafkactlProperties.getUserToken()).thenReturn("userToken");

        Optional<String> actual = configService.getMissingCurrentContextProperty();

        assertEquals(Optional.empty(), actual);
    }

    @Test
    void shouldGetMissingApi() {
        when(kafkactlProperties.getApi()).thenReturn(" ");

        Optional<String> actual = configService.getMissingCurrentContextProperty();

        assertEquals(Optional.of("API"), actual);
    }

    @Test
    void shouldGetMissingUserToken() {
        when(kafkactlProperties.getApi()).thenReturn("https://ns4kafka.com");
        when(kafkactlProperties.getUserToken()).thenReturn(" ");

        Optional<String> actual = configService.getMissingCurrentContextProperty();

        assertEquals(Optional.of("user token"), actual);
    }

    @Test
    void shouldApplyContextByName() throws IOException {
        KafkactlProperties.ContextsProperties contextPropertiesOne = KafkactlProperties.ContextsProperties.builder()
                .name("context1")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("token1")
                        .api("https://ns4kafka1.com")
                        .namespace("namespace1")
                        .build())
                .build();

        KafkactlProperties.ContextsProperties contextPropertiesTwo = KafkactlProperties.ContextsProperties.builder()
                .name("context2")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("token2")
                        .api("https://ns4kafka2.com")
                        .namespace("namespace2")
                        .build())
                .build();

        when(kafkactlProperties.getContexts()).thenReturn(List.of(contextPropertiesOne, contextPropertiesTwo));

        boolean actual = configService.applyContextByName("context2");

        assertTrue(actual);
        verify(loginService).deleteJwtFile();
    }

    @Test
    void shouldNotApplyContextByNameWhenNotFound() throws IOException {
        KafkactlProperties.ContextsProperties contextPropertiesOne = KafkactlProperties.ContextsProperties.builder()
                .name("context1")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .userToken("token1")
                        .api("https://ns4kafka1.com")
                        .namespace("namespace1")
                        .build())
                .build();

        when(kafkactlProperties.getContexts()).thenReturn(List.of(contextPropertiesOne));

        boolean actual = configService.applyContextByName("notFound");

        assertFalse(actual);
        verify(loginService, never()).deleteJwtFile();
    }
}
