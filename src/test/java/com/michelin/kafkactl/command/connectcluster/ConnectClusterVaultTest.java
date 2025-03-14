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
package com.michelin.kafkactl.command.connectcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ConnectClusterVaultTest {
    @Mock
    KafkactlConfig kafkactlConfig;

    @Mock
    ResourceService resourceService;

    @Mock
    ConfigService configService;

    @Mock
    LoginService loginService;

    @Mock
    Kafkactl kafkactl;

    @InjectMocks
    ConnectClusterVault subcommand;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(false);

        int code = cmd.execute();
        assertEquals(1, code);
        assertTrue(sw.toString()
                .contains("No valid current context found. "
                        + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotExecuteWhenNotLoggedIn() {
        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(false);

        int code = cmd.execute();
        assertEquals(1, code);
    }

    @Test
    void shouldListAvailableVaultsClustersSuccess() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");
        when(resourceService.listAvailableVaultsConnectClusters(any(), any())).thenReturn(0);

        CommandLine cmd = new CommandLine(subcommand);

        int code = cmd.execute();
        assertEquals(0, code);
        verify(resourceService).listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());
    }

    @Test
    void shouldListAvailableVaultsClustersFail() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");
        when(resourceService.listAvailableVaultsConnectClusters(any(), any())).thenReturn(1);

        CommandLine cmd = new CommandLine(subcommand);

        int code = cmd.execute();
        assertEquals(1, code);
        verify(resourceService).listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());
    }

    @Test
    void shouldDisplayErrorMessageWhenNoSecretsPassed() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("connectCluster");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("No secrets to encrypt."));
    }

    @Test
    void shouldVaultSuccess() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");

        when(resourceService.vaultsOnConnectClusters(any(), any(), any(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(subcommand);

        int code = cmd.execute("connectCluster", "secret1", "secret2");
        assertEquals(0, code);
        verify(resourceService)
                .vaultsOnConnectClusters(
                        "namespace", "connectCluster", List.of("secret1", "secret2"), cmd.getCommandSpec());
    }
}
