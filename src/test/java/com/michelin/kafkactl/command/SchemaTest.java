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
package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.SCHEMA_COMPATIBILITY_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.property.KafkactlProperties;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
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

@ExtendWith(MockitoExtension.class)
class SchemaTest {
    @Mock
    LoginService loginService;

    @Mock
    ResourceService resourceService;

    @Mock
    ConfigService configService;

    @Mock
    KafkactlProperties kafkactlProperties;

    @Mock
    Kafkactl kafkactl;

    @Mock
    FormatService formatService;

    @InjectMocks
    Schema schema;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(schema);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(false);

        int code = cmd.execute("backward", "mySubject");
        assertEquals(1, code);
        assertTrue(sw.toString()
                .contains("No valid current context found. "
                        + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotUpdateCompatWhenNotAuthenticated() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(false);

        CommandLine cmd = new CommandLine(schema);

        int code = cmd.execute("backward", "mySubject");
        assertEquals(1, code);
    }

    @Test
    void shouldNotUpdateCompatWhenEmptyResponseList() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(resourceService.changeSchemaCompatibility(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");

        CommandLine cmd = new CommandLine(schema);

        int code = cmd.execute("backward", "mySubject");
        assertEquals(1, code);
    }

    @Test
    void shouldUpdateCompat() {
        Resource resource = Resource.builder()
                .kind("SchemaCompatibilityState")
                .apiVersion("v1")
                .metadata(Metadata.builder()
                        .name("prefix.schema-value")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(resourceService.changeSchemaCompatibility(any(), any(), any(), any()))
                .thenReturn(Optional.of(resource));

        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");

        CommandLine cmd = new CommandLine(schema);

        int code = cmd.execute("backward", "mySubject", "-n", "namespace");
        assertEquals(0, code);
        verify(formatService)
                .displayList(
                        eq(SCHEMA_COMPATIBILITY_STATE),
                        argThat(schemas -> schemas.getFirst().equals(resource)),
                        eq(TABLE),
                        eq(cmd.getCommandSpec()));
    }
}
