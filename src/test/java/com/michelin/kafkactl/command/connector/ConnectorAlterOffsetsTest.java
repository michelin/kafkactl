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

import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECTOR_RESET_OFFSETS_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.property.KafkactlProperties;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ConnectorAlterOffsetsTest {
    @Mock
    LoginService loginService;

    @Mock
    KafkactlProperties kafkactlProperties;

    @Mock
    ResourceService resourceService;

    @Mock
    FormatService formatService;

    @Mock
    ConfigService configService;

    @InjectMocks
    ConnectorAlterOffsets connectorAlterOffsets;

    @TempDir
    Path tempDir;

    @Test
    void shouldAlterConnectorOffsets() throws IOException {
        Path offsetsFile = writeOffsetsFile();
        Resource response = Resource.builder()
                .kind(CONNECTOR_RESET_OFFSETS_RESPONSE)
                .metadata(Resource.Metadata.builder().name("my-connector").build())
                .status(Map.of("code", "RESET"))
                .build();

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(resourceService.alterConnectorOffsets(any(), any(), any(), any())).thenReturn(Optional.of(response));

        CommandLine cmd = new CommandLine(connectorAlterOffsets);
        int code = cmd.execute("my-connector", "-f", offsetsFile.toString(), "-n", "namespace");

        assertEquals(0, code);
        verify(resourceService)
                .alterConnectorOffsets(
                        eq("namespace"),
                        eq("my-connector"),
                        argThat(request -> request.getOffsets().size() == 1
                                && request.getOffsets()
                                        .getFirst()
                                        .getPartition()
                                        .equals(Map.of("kafka_topic", "topic1", "kafka_partition", 0))
                                && request.getOffsets().getFirst().getOffset() == null),
                        eq(cmd.getCommandSpec()));
        verify(formatService)
                .displayList(CONNECTOR_RESET_OFFSETS_RESPONSE, List.of(response), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldReturnFailureWhenResponseIsEmpty() throws IOException {
        Path offsetsFile = writeOffsetsFile();

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(resourceService.alterConnectorOffsets(any(), any(), any(), any())).thenReturn(Optional.empty());

        CommandLine cmd = new CommandLine(connectorAlterOffsets);
        int code = cmd.execute("my-connector", "-f", offsetsFile.toString(), "-n", "namespace");

        assertEquals(1, code);
    }

    private Path writeOffsetsFile() throws IOException {
        return Files.writeString(tempDir.resolve("offsets.yaml"), """
                offsets:
                  - partition:
                      kafka_topic: topic1
                      kafka_partition: 0
                    offset: null
                """);
    }
}
