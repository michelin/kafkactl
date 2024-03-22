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

import com.michelin.kafkactl.KafkactlCommand;
import com.michelin.kafkactl.client.BearerAccessRefreshToken;
import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.UserInfoResponse;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.model.JwtContent;
import com.michelin.kafkactl.model.Resource;
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
    public ClusterResourceClient clusterResourceClient;

    @Mock
    public KafkactlConfig kafkactlConfig;

    private String backUpJwt;

    @BeforeEach
    void setUp() throws IOException {
        backUpJwt = Files.readString(Paths.get("src/test/resources/fake_login/jwt"));
    }

    @AfterEach
    void tearDown() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("src/test/resources/fake_login/jwt", false));
        writer.append(backUpJwt);
        writer.close();
    }

    @Test
    void shouldNotBeAuthenticatedWhenJwtNotExist() {
        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources");

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);
        assertTrue(sw.toString().isBlank());
        assertFalse(loginService.isAuthenticated(cmd.getCommandSpec(), false));
    }

    @Test
    void shouldNotBeAuthenticatedWhenThrowUnauthorized() {
        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.unauthorized());
        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");
        when(clusterResourceClient.tokenInfo(any()))
            .thenThrow(exception);

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);
        boolean actual = loginService.isAuthenticated(cmd.getCommandSpec(), true);
        assertTrue(sw.toString().isBlank());
        assertFalse(actual);
    }

    @Test
    void shouldNotBeAuthenticatedWhenThrowException() {
        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");
        when(clusterResourceClient.tokenInfo(any()))
            .thenThrow(exception);

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);
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

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");
        when(clusterResourceClient.tokenInfo(any()))
            .thenReturn(userInfoResponse);

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);
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

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");
        when(clusterResourceClient.tokenInfo(any()))
            .thenReturn(userInfoResponse);

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);

        boolean actual = loginService.isAuthenticated(cmd.getCommandSpec(), true);
        assertTrue(sw.toString().contains("Authentication reused. Welcome username!"));
        assertTrue(sw.toString().contains("Your session is valid until"));
        assertTrue(actual);

        String token = loginService.getAuthorization();
        assertTrue(token.startsWith("Bearer eyJhbGciO"));
    }

    @Test
    void shouldNotLoginWhenHttpClientResponseException() {
        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");
        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(clusterResourceClient.login(any()))
            .thenThrow(exception);

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);
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

        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");
        when(clusterResourceClient.login(any()))
            .thenReturn(bearerAccessRefreshToken);

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);

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

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");
        when(clusterResourceClient.tokenInfo(any()))
            .thenReturn(userInfoResponse);

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);

        boolean actual = loginService.doAuthenticate(cmd.getCommandSpec(), true);
        assertTrue(sw.toString().contains("Authentication reused. Welcome username!"));
        assertTrue(sw.toString().contains("Your session is valid until"));
        assertTrue(actual);

        String token = loginService.getAuthorization();
        assertTrue(token.startsWith("Bearer eyJhbGciOiJIUtI1N"));
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

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");
        when(clusterResourceClient.tokenInfo(any()))
            .thenReturn(userInfoResponse);
        when(clusterResourceClient.login(any()))
            .thenReturn(bearerAccessRefreshToken);

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);

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

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");
        when(clusterResourceClient.tokenInfo(any()))
            .thenReturn(userInfoResponse);
        when(clusterResourceClient.login(any()))
            .thenThrow(exception);

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);

        boolean actual = loginService.doAuthenticate(cmd.getCommandSpec(), false);
        assertFalse(actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReadJwtFile() throws IOException {
        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);
        List<Resource> actual = loginService.readJwtFile();

        assertEquals(2, actual.size());
        assertEquals("anotherNamespace", actual.get(0).getSpec().get("namespace"));
        assertIterableEquals(List.of(GET), (List<JwtContent.RoleBinding.Verb>) actual.get(0).getSpec().get("verbs"));
        assertIterableEquals(List.of("quota"), (List<String>) actual.get(0).getSpec().get("resources"));

        assertEquals("anotherNamespace", actual.get(1).getSpec().get("namespace"));
        assertIterableEquals(List.of(GET, POST, PUT, DELETE),
            (List<JwtContent.RoleBinding.Verb>) actual.get(1).getSpec().get("verbs"));
        assertIterableEquals(List.of("schemas", "schemas/config", "topics", "topics/import", "topics/delete-records",
            "connectors", "connectors/import", "connectors/change-state", "connect-clusters", "connect-clusters/vaults",
            "acls", "consumer-groups/reset", "streams"), (List<String>) actual.get(1).getSpec().get("resources"));
    }

    @Test
    void shouldJwtExists() {
        when(kafkactlConfig.getConfigDirectory())
            .thenReturn("src/test/resources/fake_login");

        LoginService loginService = new LoginService(kafkactlConfig, clusterResourceClient);
        assertTrue(loginService.jwtFileExists());
    }
}
