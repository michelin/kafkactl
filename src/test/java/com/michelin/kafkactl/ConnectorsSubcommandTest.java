package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
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
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectorsSubcommandTest {
    @Mock
    public LoginService loginService;

    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public ResourceService resourceService;

    @Mock
    public ApiResourcesService apiResourcesService;

    @Mock
    public FormatService formatService;

    @Mock
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ConnectorsSubcommand connectorsSubcommand;

    @Test
    void shouldNotChangeStateWhenNotAuthenticated() {
        when(loginService.doAuthenticate())
                .thenReturn(false);

        CommandLine cmd = new CommandLine(connectorsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("pause", "my-connector");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Login failed."));
    }

    @Test
    void shouldChangeStateWhenEmptyConnectorsList() {
        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(resourceService.changeConnectorState(any(), any(), any()))
                .thenReturn(null);

        kafkactlCommand.optionalNamespace = Optional.of("namespace");

        CommandLine cmd = new CommandLine(connectorsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("pause", "my-connector");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("Cannot change state of given connectors."));
    }

    @Test
    void shouldChangeState() {
        Resource resource = Resource.builder()
                .kind("Connector")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.connector")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(resourceService.changeConnectorState(any(), any(), any()))
                .thenReturn(resource);
        doAnswer(answer -> {
            PrintWriter cdmPrintWriter = answer.getArgument(3, PrintWriter.class);
            cdmPrintWriter.println("Called display list");
            return null;
        }).when(formatService).displayList(any(), any(), any(), any());

        kafkactlCommand.optionalNamespace = Optional.of("namespace");

        CommandLine cmd = new CommandLine(connectorsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("pause", "my-connector");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Called display list"));
    }
}
