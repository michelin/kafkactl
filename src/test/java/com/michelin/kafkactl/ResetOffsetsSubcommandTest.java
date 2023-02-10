package com.michelin.kafkactl;

import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetOffsetsSubcommandTest {
    @Mock
    public LoginService loginService;

    @Mock
    public ResourceService resourceService;

    @Mock
    public FormatService formatService;

    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ResetOffsetsSubcommand resetOffsetsSubcommand;

    @Test
    void shouldNotResetWhenNotAuthenticated() {
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(false);

        CommandLine cmd = new CommandLine(resetOffsetsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("--group", "myGroup", "--all-topics", "--to-earliest");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("Login failed."));
    }
}
