package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectorsSubcommandTest {
    @Mock
    private LoginService loginService;

    @Mock
    private KafkactlConfig kafkactlConfig;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ApiResourcesService apiResourcesService;

    @Mock
    private FormatService formatService;

    @Mock
    private KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ConnectorsSubcommand connectorsSubcommand;

    @Test
    void shouldNotChangeStateWhenNotAuthenticated() {
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(false);

        CommandLine cmd = new CommandLine(connectorsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("pause", "my-connector");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("Login failed."));
    }

    @Test
    void shouldChangeStateWhenEmptyConnectorsList() {
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(resourceService.changeConnectorState(any(), any(), any(), any()))
                .thenReturn(null);

        kafkactlCommand.optionalNamespace = Optional.empty();

        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");

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

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(resourceService.changeConnectorState(any(), any(), any(), any()))
                .thenReturn(resource);

        kafkactlCommand.optionalNamespace = Optional.of("namespace");

        CommandLine cmd = new CommandLine(connectorsSubcommand);

        int code = cmd.execute("pause", "my-connector");
        assertEquals(0, code);
        verify(formatService).displayList(eq("ChangeConnectorState"),
                argThat(connectors -> connectors.get(0).equals(resource)),
                eq(TABLE), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldChangeStateOfAll() {
        Resource resource = Resource.builder()
                .kind("Connector")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.connector")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        ApiResource apiResource = ApiResource.builder()
                .kind("Connector")
                .namespaced(true)
                .synchronizable(true)
                .path("connectors")
                .names(List.of("connects", "connect", "co"))
                .build();

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionFromKind(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.listResourcesWithType(any(), any(), any()))
                .thenReturn(Collections.singletonList(resource));
        when(resourceService.changeConnectorState(any(), any(), any(), any()))
                .thenReturn(resource);

        kafkactlCommand.optionalNamespace = Optional.of("namespace");

        CommandLine cmd = new CommandLine(connectorsSubcommand);

        int code = cmd.execute("pause", "all");
        assertEquals(0, code);
        verify(formatService).displayList(eq("ChangeConnectorState"),
                argThat(connectors -> connectors.get(0).equals(resource)),
                eq(TABLE), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldThrowExceptionWhenChangeStateOfAll() {
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionFromKind(any()))
                .thenReturn(Optional.empty());

        kafkactlCommand.optionalNamespace = Optional.of("namespace");

        CommandLine cmd = new CommandLine(connectorsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("pause", "all");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type Connector."));
    }
}
