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
package com.michelin.kafkactl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.model.ServerInfo;
import com.michelin.kafkactl.property.KafkactlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VersionProviderTest {
    @Mock
    KafkactlProperties kafkactlProperties;

    @Mock
    ClusterResourceClient clusterResourceClient;

    @InjectMocks
    VersionProvider versionProvider;

    @Test
    void shouldGetVersion() {
        when(kafkactlProperties.getVersion()).thenReturn("1.0.0");
        when(clusterResourceClient.serverInfo()).thenReturn(new ServerInfo("1.0.0"));

        String[] actual = versionProvider.getVersion();

        assertEquals("Client Version: v1.0.0", actual[0]);
        assertEquals("Server Version: v1.0.0", actual[1]);
    }

    @Test
    void shouldGetVersionWhenServerDoesNotRespond() {
        when(kafkactlProperties.getVersion()).thenReturn("1.0.0");
        when(clusterResourceClient.serverInfo()).thenThrow(new RuntimeException("Server not reachable"));

        String[] actual = versionProvider.getVersion();

        assertEquals("Client Version: v1.0.0", actual[0]);
        assertEquals("Server Version: Server not reachable", actual[1]);
    }
}
