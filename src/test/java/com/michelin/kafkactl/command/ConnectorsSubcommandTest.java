package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.CHANGE_CONNECTOR_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.command.ConnectorsSubcommand;
import com.michelin.kafkactl.command.KafkactlCommand;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

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
    private ConfigService configService;

    @Mock
    private KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ConnectorsSubcommand connectorsSubcommand;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(connectorsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(false);

        int code = cmd.execute("pause", "my-connector");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("No valid current context found. "
            + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotChangeStateWhenNotAuthenticated() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(false);

        CommandLine cmd = new CommandLine(connectorsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("pause", "my-connector");
        assertEquals(1, code);
    }

    @Test
    void shouldNotChangeStateWhenEmptyConnectorsList() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(resourceService.changeConnectorState(any(), any(), any(), any()))
            .thenReturn(Optional.empty());

        kafkactlCommand.optionalNamespace = Optional.empty();

        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");

        CommandLine cmd = new CommandLine(connectorsSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("pause", "my-connector");
        assertEquals(1, code);
    }

    @Test
    void shouldChangeState() {
        Resource resource = Resource.builder()
            .kind("Connector")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.connector")
                .namespace("namespace")
                .build())
            .spec(Collections.emptyMap())
            .build();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(resourceService.changeConnectorState(any(), any(), any(), any()))
            .thenReturn(Optional.of(resource));

        kafkactlCommand.optionalNamespace = Optional.of("namespace");

        CommandLine cmd = new CommandLine(connectorsSubcommand);

        int code = cmd.execute("pause", "my-connector");
        assertEquals(0, code);
        verify(formatService).displayList(eq(CHANGE_CONNECTOR_STATE),
            argThat(connectors -> connectors.get(0).equals(resource)),
            eq(TABLE), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldChangeStateOfAll() {
        Resource resource = Resource.builder()
            .kind("Connector")
            .apiVersion("v1")
            .metadata(Metadata.builder()
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

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByKind(any()))
            .thenReturn(Optional.of(apiResource));
        when(resourceService.listResourcesWithType(any(), any()))
            .thenReturn(Collections.singletonList(resource));
        when(resourceService.changeConnectorState(any(), any(), any(), any()))
            .thenReturn(Optional.of(resource));

        kafkactlCommand.optionalNamespace = Optional.of("namespace");

        CommandLine cmd = new CommandLine(connectorsSubcommand);

        int code = cmd.execute("pause", "all");
        assertEquals(0, code);
        verify(formatService).displayList(eq(CHANGE_CONNECTOR_STATE),
            argThat(connectors -> connectors.get(0).equals(resource)),
            eq(TABLE), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldNotChangeStateWhenHttpClientResponseException() {
        kafkactlCommand.optionalNamespace = Optional.of("namespace");

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        ApiResource apiResource = ApiResource.builder()
            .kind("Connector")
            .namespaced(true)
            .synchronizable(true)
            .path("connectors")
            .names(List.of("connects", "connect", "co"))
            .build();

        when(apiResourcesService.getResourceDefinitionByKind(any()))
            .thenReturn(Optional.of(apiResource));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());

        when(resourceService.listResourcesWithType(any(), any()))
            .thenThrow(exception);

        CommandLine cmd = new CommandLine(connectorsSubcommand);

        int code = cmd.execute("pause", "all");
        assertEquals(1, code);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldThrowExceptionWhenChangeStateOfAll() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByKind(any()))
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