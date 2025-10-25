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
package com.michelin.kafkactl.service;

import static com.michelin.kafkactl.model.JwtContent.RoleBinding.Verb.DELETE;
import static com.michelin.kafkactl.model.JwtContent.RoleBinding.Verb.GET;
import static com.michelin.kafkactl.model.JwtContent.RoleBinding.Verb.POST;
import static com.michelin.kafkactl.model.JwtContent.RoleBinding.Verb.PUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.client.BearerAccessRefreshToken;
import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.UserInfoResponse;
import com.michelin.kafkactl.model.JwtContent;
import com.michelin.kafkactl.property.KafkactlProperties;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {
    @Mock
    ClusterResourceClient clusterResourceClient;

    @Mock
    KafkactlProperties kafkactlProperties;

    String backUpJwt;

    @BeforeEach
    void setUp() throws IOException {
        backUpJwt = Files.readString(Paths.get("src/test/resources/fake-login/jwt"));
    }

    @AfterEach
    void tearDown() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("src/test/resources/fake-login/jwt", false));
        writer.append(backUpJwt);
        writer.close();
    }

    @Test
    void shouldNotBeAuthenticatedWhenJwtNotExist() {
        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources");

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);
        assertTrue(sw.toString().isBlank());
        assertFalse(loginService.isAuthenticated(cmd.getCommandSpec(), false));
    }

    @Test
    void shouldNotBeAuthenticatedWhenThrowUnauthorized() {
        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.unauthorized());
        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");
        when(clusterResourceClient.tokenInfo(any())).thenThrow(exception);

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);
        boolean actual = loginService.isAuthenticated(cmd.getCommandSpec(), true);
        assertTrue(sw.toString().isBlank());
        assertFalse(actual);
    }

    @Test
    void shouldNotBeAuthenticatedWhenThrowException() {
        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");
        when(clusterResourceClient.tokenInfo(any())).thenThrow(exception);

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);
        boolean actual = loginService.isAuthenticated(cmd.getCommandSpec(), true);
        assertTrue(sw.toString().contains("Unexpected error occurred: error (500)."));
        assertFalse(actual);
    }

    @Test
    void shouldNotBeAuthenticatedWhenInactive() {
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        userInfoResponse.setUsername("username");
        userInfoResponse.setExp(10);
        userInfoResponse.setActive(false);

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");
        when(clusterResourceClient.tokenInfo(any())).thenReturn(userInfoResponse);

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);
        boolean actual = loginService.isAuthenticated(cmd.getCommandSpec(), true);
        assertTrue(sw.toString().contains("Authentication reused. Welcome username!"));
        assertTrue(sw.toString().contains("Your session is valid until"));
        assertFalse(actual);
    }

    @Test
    void shouldBeAuthenticated() {
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        userInfoResponse.setUsername("username");
        userInfoResponse.setExp(10);
        userInfoResponse.setActive(true);

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");
        when(clusterResourceClient.tokenInfo(any())).thenReturn(userInfoResponse);

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);

        boolean actual = loginService.isAuthenticated(cmd.getCommandSpec(), true);
        assertTrue(sw.toString().contains("Authentication reused. Welcome username!"));
        assertTrue(sw.toString().contains("Your session is valid until"));
        assertTrue(actual);

        String token = loginService.getAuthorization();
        assertTrue(token.startsWith("Bearer eyJhbGciO"));
    }

    @Test
    void shouldNotLoginWhenHttpClientResponseException() {
        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");
        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(clusterResourceClient.login(any())).thenThrow(exception);

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);
        boolean actual = loginService.login(cmd.getCommandSpec(), "username", "passwd", false);
        assertTrue(sw.toString().contains("Authentication failed because error."));
        assertFalse(actual);
    }

    @Test
    void shouldLogin() {
        BearerAccessRefreshToken bearerAccessRefreshToken = new BearerAccessRefreshToken();
        bearerAccessRefreshToken.setUsername("username");
        bearerAccessRefreshToken.setAccessToken("accessToken");
        bearerAccessRefreshToken.setTokenType("tokenType");
        bearerAccessRefreshToken.setExpiresIn(1);
        bearerAccessRefreshToken.setRoles(Collections.singletonList("user"));

        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");
        when(clusterResourceClient.login(any())).thenReturn(bearerAccessRefreshToken);

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);

        boolean actual = loginService.login(cmd.getCommandSpec(), "username", "passwd", true);
        assertTrue(actual);
        assertTrue(sw.toString().contains("Authenticating..."));
        assertTrue(sw.toString().contains("Authentication successful. Welcome username!"));
        assertTrue(sw.toString().contains("Your session is valid until"));

        String token = loginService.getAuthorization();
        assertEquals("Bearer accessToken", token);
    }

    @Test
    void shouldDoAuthenticateWhenAlreadyAuthenticated() {
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        userInfoResponse.setUsername("username");
        userInfoResponse.setExp(10);
        userInfoResponse.setActive(true);

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");
        when(clusterResourceClient.tokenInfo(any())).thenReturn(userInfoResponse);

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);

        boolean actual = loginService.doAuthenticate(cmd.getCommandSpec(), true);
        assertTrue(sw.toString().contains("Authentication reused. Welcome username!"));
        assertTrue(sw.toString().contains("Your session is valid until"));
        assertTrue(actual);

        String token = loginService.getAuthorization();
        assertTrue(token.startsWith("Bearer eyJ"));
    }

    @Test
    void shouldDoAuthenticateWhenNotAlreadyAuthenticated() {
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        userInfoResponse.setUsername("username");
        userInfoResponse.setExp(10);
        userInfoResponse.setActive(false);

        BearerAccessRefreshToken bearerAccessRefreshToken = new BearerAccessRefreshToken();
        bearerAccessRefreshToken.setUsername("username");
        bearerAccessRefreshToken.setAccessToken("accessToken");
        bearerAccessRefreshToken.setTokenType("tokenType");
        bearerAccessRefreshToken.setExpiresIn(1);
        bearerAccessRefreshToken.setRoles(Collections.singletonList("user"));

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");
        when(clusterResourceClient.tokenInfo(any())).thenReturn(userInfoResponse);
        when(clusterResourceClient.login(any())).thenReturn(bearerAccessRefreshToken);

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);

        boolean actual = loginService.doAuthenticate(cmd.getCommandSpec(), false);
        assertTrue(actual);

        String token = loginService.getAuthorization();
        assertEquals("Bearer accessToken", token);
    }

    @Test
    void shouldDoAuthenticateAndCannotAuthenticate() {
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        userInfoResponse.setUsername("username");
        userInfoResponse.setExp(10);
        userInfoResponse.setActive(false);

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");
        when(clusterResourceClient.tokenInfo(any())).thenReturn(userInfoResponse);
        when(clusterResourceClient.login(any())).thenThrow(exception);

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);

        boolean actual = loginService.doAuthenticate(cmd.getCommandSpec(), false);
        assertFalse(actual);
    }

    @Test
    void shouldReadJwtFile() throws IOException {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);
        JwtContent actual = loginService.readJwtFile();

        assertEquals("admin", actual.getSub());
        assertEquals(1711313691L, actual.getExp());

        assertIterableEquals(List.of("isAdmin()"), actual.getRoles());

        assertIterableEquals(
                List.of("anotherNamespace"), actual.getRoleBindings().getFirst().getNamespaces());
        assertIterableEquals(List.of(GET), actual.getRoleBindings().getFirst().getVerbs());
        assertIterableEquals(
                List.of("quota"), actual.getRoleBindings().getFirst().getResourceTypes());

        assertIterableEquals(
                List.of("anotherNamespace"), actual.getRoleBindings().get(1).getNamespaces());
        assertIterableEquals(
                List.of(GET, POST, PUT, DELETE), actual.getRoleBindings().get(1).getVerbs());
        assertIterableEquals(
                List.of(
                        "schemas",
                        "schemas/config",
                        "topics",
                        "topics/import",
                        "topics/delete-records",
                        "connectors",
                        "connectors/import",
                        "connectors/change-state",
                        "connect-clusters",
                        "connect-clusters/vaults",
                        "acls",
                        "consumer-groups/reset",
                        "streams"),
                actual.getRoleBindings().get(1).getResourceTypes());
    }

    @Test
    void shouldJwtExists() {
        when(kafkactlProperties.getConfigDirectory()).thenReturn("src/test/resources/fake-login");

        LoginService loginService = new LoginService(kafkactlProperties, clusterResourceClient);
        assertTrue(loginService.jwtFileExists());
    }
}
