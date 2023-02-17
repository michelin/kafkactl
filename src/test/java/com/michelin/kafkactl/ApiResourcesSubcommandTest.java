package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
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
import java.util.List;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.RESOURCE_DEFINITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiResourcesSubcommandTest {
    @Mock
    private ApiResourcesService apiResourcesService;

    @Mock
    private LoginService loginService;

    @Mock
    private KafkactlCommand kafkactlCommand;

    @Mock
    public FormatService formatService;

    @InjectMocks
    private ApiResourcesSubcommand apiResourcesSubcommand;

    @Test
    void shouldNotDisplayApiResourcesWhenNotAuthenticated() {
        CommandLine cmd = new CommandLine(apiResourcesSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(false);

        int code = cmd.execute();
        assertEquals(1, code);
    }

    @Test
    void shouldDisplayApiResources() {
      ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        CommandLine cmd = new CommandLine(apiResourcesSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.listResourceDefinitions())
                .thenReturn(Collections.singletonList(apiResource));

        int code = cmd.execute();
        assertEquals(0, code);
        verify(formatService).displayList(eq(RESOURCE_DEFINITION),
                argThat(resources -> resources.get(0).getMetadata().getName().equals("Topic")),
                eq(TABLE), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldDisplayError() {
        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());

        CommandLine cmd = new CommandLine(apiResourcesSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.listResourceDefinitions())
                .thenThrow(exception);

        int code = cmd.execute();
        assertEquals(1, code);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }
}
