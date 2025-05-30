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

import static com.michelin.kafkactl.model.Output.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.property.KafkactlProperties;
import com.michelin.kafkactl.service.FormatService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

/** Config get contexts subcommand test. */
@ExtendWith(MockitoExtension.class)
class ConfigGetContextsTest {
    @Mock
    KafkactlProperties kafkactlProperties;

    @Mock
    FormatService formatService;

    @InjectMocks
    ConfigGetContexts subcommand;

    @Test
    void shouldGetEmptyContexts() {
        when(kafkactlProperties.getContexts()).thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        assertTrue(sw.toString().contains("No context pre-defined."));
    }

    @Test
    void shouldGetContextsWithMaskedTokens() {
        KafkactlProperties.ContextsProperties contextProperties = KafkactlProperties.ContextsProperties.builder()
                .name("name")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .api("api")
                        .namespace("namespace")
                        .userToken("userToken")
                        .build())
                .build();

        when(kafkactlProperties.getContexts()).thenReturn(Collections.singletonList(contextProperties));

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        verify(formatService)
                .displayList(
                        eq("Context"),
                        argThat(currentContext -> currentContext
                                        .getFirst()
                                        .getMetadata()
                                        .getName()
                                        .equals("name")
                                && currentContext
                                        .getFirst()
                                        .getSpec()
                                        .get("token")
                                        .equals("[MASKED]")),
                        eq(TABLE),
                        eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldGetContextsWithUnmaskedTokens() {
        KafkactlProperties.ContextsProperties contextProperties = KafkactlProperties.ContextsProperties.builder()
                .name("name")
                .context(KafkactlProperties.ContextsProperties.ContextProperties.builder()
                        .api("api")
                        .namespace("namespace")
                        .userToken("userToken")
                        .build())
                .build();

        when(kafkactlProperties.getContexts()).thenReturn(Collections.singletonList(contextProperties));

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-u");
        assertEquals(0, code);
        verify(formatService)
                .displayList(
                        eq("Context"),
                        argThat(currentContext -> currentContext
                                        .getFirst()
                                        .getMetadata()
                                        .getName()
                                        .equals("name")
                                && currentContext
                                        .getFirst()
                                        .getSpec()
                                        .get("token")
                                        .equals("userToken")),
                        eq(TABLE),
                        eq(cmd.getCommandSpec()));
    }
}
