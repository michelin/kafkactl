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
package com.michelin.kafkactl.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.michelin.kafkactl.property.KafkactlProperties;
import com.michelin.kafkactl.property.KafkactlProperties.ContextsProperties;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
class ConfigIntegrationTest {
    @Inject
    private KafkactlProperties kafkactlProperties;

    @Test
    void shouldBindConfigurationFromConfigFile() {
        assertEquals("https://ns4kafka.com", kafkactlProperties.getApi());
        assertEquals("userToken", kafkactlProperties.getUserToken());
        assertEquals("namespace", kafkactlProperties.getCurrentNamespace());
        assertEquals(2, kafkactlProperties.getContexts().size());

        ContextsProperties dev = kafkactlProperties.getContexts().getFirst();
        assertEquals("dev", dev.getName());
        assertEquals("https://ns4kafka-dev-api.domain.com", dev.getContext().getApi());
        assertEquals("my_token", dev.getContext().getUserToken());
        assertEquals("my_namespace", dev.getContext().getNamespace());

        ContextsProperties prod = kafkactlProperties.getContexts().getLast();
        assertEquals("prod", prod.getName());
        assertEquals("https://ns4kafka-prod-api.domain.com", prod.getContext().getApi());
    }
}
