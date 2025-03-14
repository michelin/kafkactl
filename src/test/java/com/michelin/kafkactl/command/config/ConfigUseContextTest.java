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
package com.michelin.kafkactl.command.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.service.ConfigService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

/** Config use context subcommand test. */
@ExtendWith(MockitoExtension.class)
class ConfigUseContextTest {
    @Mock
    KafkactlConfig kafkactlConfig;

    @Mock
    ConfigService configService;

    @InjectMocks
    ConfigUseContext subcommand;

    @Test
    void shouldGetEmptyContexts() {
        when(kafkactlConfig.getContexts()).thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        assertTrue(sw.toString().contains("No context pre-defined."));
    }

    @Test
    void shouldUseContextNotFound() {
        KafkactlConfig.Context context = KafkactlConfig.Context.builder()
                .name("name")
                .definition(KafkactlConfig.Context.ApiContext.builder()
                        .api("api")
                        .namespace("namespace")
                        .userToken("userToken")
                        .build())
                .build();

        when(kafkactlConfig.getContexts()).thenReturn(Collections.singletonList(context));
        when(configService.getContextByName(any())).thenReturn(Optional.empty());

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("context");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("No context exists with the name \"context\"."));
    }

    @Test
    void shouldUseContext() {
        KafkactlConfig.Context context = KafkactlConfig.Context.builder()
                .name("name")
                .definition(KafkactlConfig.Context.ApiContext.builder()
                        .api("api")
                        .namespace("namespace")
                        .userToken("userToken")
                        .build())
                .build();

        when(kafkactlConfig.getContexts()).thenReturn(Collections.singletonList(context));
        when(configService.getContextByName(any())).thenReturn(Optional.of(context));

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("context");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Switched to context \"context\"."));
    }
}
