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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ResetPasswordTest {
    @Mock
    LoginService loginService;

    @Mock
    ResourceService resourceService;

    @Mock
    ConfigService configService;

    @Mock
    KafkactlConfig kafkactlConfig;

    @Mock
    Kafkactl kafkactl;

    @InjectMocks
    ResetPassword resetPassword;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(resetPassword);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid()).thenReturn(false);

        int code = cmd.execute("user");
        assertEquals(1, code);
        assertTrue(sw.toString()
                .contains("No valid current context found. "
                        + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotUpdateUserWhenNotAuthenticated() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(false);

        CommandLine cmd = new CommandLine(resetPassword);

        int code = cmd.execute("user");
        assertEquals(1, code);
    }

    @Test
    void shouldNotUpdateUserWhenUnknownOutput() {
        CommandLine cmd = new CommandLine(resetPassword);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("user", "-o", "unknownOutputFormat");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Invalid value for option '--output'"));
    }

    @Test
    void shouldNotUpdateUserWhenNotConfirmed() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");

        CommandLine cmd = new CommandLine(resetPassword);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("user");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("You are about to change your Kafka password for the namespace namespace."));
        assertTrue(sw.toString().contains("Active connections will be killed instantly."));
        assertTrue(sw.toString().contains("To execute this operation, rerun the command with option --execute."));
        verify(resourceService, never()).resetPassword(any(), any(), any(), any());
    }

    @Test
    void shouldUpdateUser() {
        when(configService.isCurrentContextValid()).thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");
        when(resourceService.resetPassword(any(), any(), any(), any())).thenReturn(0);

        CommandLine cmd = new CommandLine(resetPassword);

        int code = cmd.execute("user", "--execute");
        assertEquals(0, code);
        verify(resourceService).resetPassword("namespace", "user", TABLE, cmd.getCommandSpec());
    }
}
