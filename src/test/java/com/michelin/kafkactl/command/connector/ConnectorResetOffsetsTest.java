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
package com.michelin.kafkactl.command.connector;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.michelin.kafkactl.model.ApiResource;
import static com.michelin.kafkactl.model.Output.TABLE;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.property.KafkactlProperties;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECTOR_RESET_OFFSETS_RESPONSE;

import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ConnectorResetOffsetsTest {
    @Mock
    LoginService loginService;

    @Mock
    KafkactlProperties kafkactlProperties;

    @Mock
    ResourceService resourceService;

    @Mock
    ApiResourcesService apiResourcesService;

    @Mock
    FormatService formatService;

    @Mock
    ConfigService configService;

    @InjectMocks
    ConnectorResetOffsets connectorResetOffsets;

    @Test
    void shouldNotResetOffsetsWhenEmptyResponse() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(resourceService.resetConnectorOffsets(any(), any(), any())).thenReturn(Optional.empty());
        when(kafkactlProperties.getCurrentNamespace()).thenReturn("namespace");

        CommandLine cmd = new CommandLine(connectorResetOffsets);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("my-connector");
        assertEquals(1, code);
    }

    @Test
    void shouldResetOffsets() {
        Resource resource = Resource.builder()
                .kind("ConnectorResetOffsetsResponse")
                .apiVersion("v1")
                .metadata(Resource.Metadata.builder()
                        .name("my-connector")
                        .namespace("namespace")
                        .build())
                .status(Map.of("code", "Offsets for connector my-connector reset successfully"))
                .build();

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(resourceService.resetConnectorOffsets(any(), any(), any())).thenReturn(Optional.of(resource));

        CommandLine cmd = new CommandLine(connectorResetOffsets);

        int code = cmd.execute("my-connector", "-n", "namespace");
        assertEquals(0, code);
        verify(formatService)
                .displayList(CONNECTOR_RESET_OFFSETS_RESPONSE, List.of(resource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldResetOffsetsOfAll() {
        Resource listedConnector = Resource.builder()
                .kind("Connector")
                .apiVersion("v1")
                .metadata(Resource.Metadata.builder()
                        .name("prefix.connector")
                        .namespace("namespace")
                        .build())
                .build();

        Resource resetResponse = Resource.builder()
                .kind("ConnectorResetOffsetsResponse")
                .apiVersion("v1")
                .status(Map.of("code", "Offsets for connector prefix.connector reset successfully"))
                .build();

        ApiResource apiResource = ApiResource.builder()
                .kind("Connector")
                .namespaced(true)
                .synchronizable(true)
                .path("connectors")
                .names(List.of("connects", "connect", "co"))
                .build();

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByKind(any())).thenReturn(Optional.of(apiResource));
        when(resourceService.listResourcesWithType(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(listedConnector));
        when(resourceService.resetConnectorOffsets(any(), any(), any())).thenReturn(Optional.of(resetResponse));

        CommandLine cmd = new CommandLine(connectorResetOffsets);

        int code = cmd.execute("all", "-n", "namespace");
        assertEquals(0, code);
        verify(formatService)
                .displayList(CONNECTOR_RESET_OFFSETS_RESPONSE, List.of(resetResponse), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(connectorResetOffsets);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(false);

        int code = cmd.execute("my-connector");
        assertEquals(1, code);
        assertTrue(sw.toString()
                .contains("No valid current context found. "
                        + "Use \"kafkactl config use-context\" to set a valid context."));
    }
}
