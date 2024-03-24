package com.michelin.kafkactl.command.auth;

import static com.michelin.kafkactl.model.JwtContent.RoleBinding.Verb.GET;
import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.AUTH_INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.model.JwtContent;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

/**
 * Auth subcommand test.
 */
@ExtendWith(MockitoExtension.class)
class AuthInfoTest {
    @Mock
    LoginService loginService;

    @Mock
    FormatService formatService;

    @InjectMocks
    AuthInfo subcommand;

    @Test
    void shouldDoNothingIfJwtNotExist() {
        when(loginService.jwtFileExists())
            .thenReturn(false);

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        assertTrue(sw.toString().contains("No JWT found. You are not authenticated."));
    }

    @Test
    void shouldDisplayInfoFromJwtAdmin() throws IOException {
        when(loginService.jwtFileExists())
            .thenReturn(true);

        JwtContent jwtContent = JwtContent.builder()
            .sub("admin")
            .exp(1711241399L)
            .roles(List.of("isAdmin()"))
            .build();

        when(loginService.readJwtFile())
            .thenReturn(jwtContent);

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Admin admin authenticated."));
        assertTrue(sw.toString().contains("Session valid until Sun Mar 24"));
    }

    @Test
    void shouldDisplayInfoFromJwtUser() throws IOException {
        when(loginService.jwtFileExists())
            .thenReturn(true);

        JwtContent jwtContent = JwtContent.builder()
            .sub("user")
            .exp(1711241399L)
            .build();

        when(loginService.readJwtFile())
            .thenReturn(jwtContent);

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        assertTrue(sw.toString().contains("User user authenticated."));
        assertTrue(sw.toString().contains("Session valid until Sun Mar 24"));
    }

    @Test
    void shouldDisplayInfoFromJwtUserAndRoleBindings() throws IOException {
        when(loginService.jwtFileExists())
            .thenReturn(true);

        JwtContent jwtContent = JwtContent.builder()
            .sub("user")
            .exp(1711241399L)
            .roleBindings(List.of(JwtContent.RoleBinding.builder()
                .namespace("namespace")
                .verbs(List.of(GET))
                .resources(List.of("resource"))
                .build()))
            .build();

        when(loginService.readJwtFile())
            .thenReturn(jwtContent);

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        assertTrue(sw.toString().contains("User user authenticated."));
        assertTrue(sw.toString().contains("Session valid until Sun Mar 24"));
        verify(formatService).displayList(AUTH_INFO, List.of(Resource.builder()
            .spec(Map.of(
                "namespace", "namespace",
                "verbs", List.of(GET),
                "resources", List.of("resource")
            ))
            .build()), TABLE, cmd.getCommandSpec());
    }
}
