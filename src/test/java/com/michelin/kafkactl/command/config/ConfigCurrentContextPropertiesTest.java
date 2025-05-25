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
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.FormatService;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

/** Config current context subcommand test. */
@ExtendWith(MockitoExtension.class)
class ConfigCurrentContextPropertiesTest {
    @Mock
    KafkactlProperties kafkactlProperties;

    @Mock
    ConfigService configService;

    @Mock
    FormatService formatService;

    @InjectMocks
    ConfigCurrentContext subcommand;

    @Test
    void shouldGetCurrentContextWithMaskedTokens() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        when(kafkactlProperties.getApi()).thenReturn("ns4kafka.com");
        when(configService.getCurrentContextName()).thenReturn("current-context");

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
                                        .equals("current-context")
                                && currentContext
                                        .getFirst()
                                        .getSpec()
                                        .get("token")
                                        .equals("[MASKED]")),
                        eq(TABLE),
                        eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldGetCurrentContextWithUnmaskedTokens() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");
        when(kafkactlProperties.getApi()).thenReturn("ns4kafka.com");
        when(kafkactlProperties.getUserToken()).thenReturn("user-token");
        when(configService.getCurrentContextName()).thenReturn("current-context");

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
                                        .equals("current-context")
                                && currentContext
                                        .getFirst()
                                        .getSpec()
                                        .get("token")
                                        .equals("user-token")),
                        eq(TABLE),
                        eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldNotGetCurrentContextWhenInvalid() {
        when(configService.isCurrentContextValid()).thenReturn(false);

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(1, code);
        assertTrue(sw.toString()
                .contains("No valid current context found. "
                        + "Use \"kafkactl config use-context\" to set a valid context."));
    }
}
