package com.michelin.kafkactl.command.auth;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.AUTH_INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
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
    void shouldDisplayInfoFromJwt() throws IOException {
        when(loginService.jwtFileExists())
            .thenReturn(true);

        Resource resource = Resource.builder()
            .metadata(Metadata.builder()
                .name("resource")
                .build())
            .build();

        when(loginService.readJwtFile())
            .thenReturn(List.of(resource));

        CommandLine cmd = new CommandLine(subcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        verify(formatService).displayList(AUTH_INFO, List.of(resource), TABLE, cmd.getCommandSpec());
    }
}
