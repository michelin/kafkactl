package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.RESOURCE_DEFINITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ApiResourcesTest {
    @Mock
    FormatService formatService;

    @Mock
    ApiResourcesService apiResourcesService;

    @Mock
    ConfigService configService;

    @Mock
    LoginService loginService;

    @InjectMocks
    private ApiResources apiResources;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(apiResources);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(false);

        int code = cmd.execute();
        assertEquals(1, code);
        assertTrue(sw.toString().contains("No valid current context found. "
            + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotDisplayApiResourcesWhenNotAuthenticated() {
        CommandLine cmd = new CommandLine(apiResources);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(true);

        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(false);

        int code = cmd.execute();
        assertEquals(1, code);
    }

    @Test
    void shouldDisplayApiResources() {
        CommandLine cmd = new CommandLine(apiResources);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(apiResourcesService.listResourceDefinitions())
            .thenReturn(Collections.singletonList(apiResource));

        int code = cmd.execute();
        assertEquals(0, code);
        verify(formatService).displayList(eq(RESOURCE_DEFINITION),
            argThat(resources -> resources.getFirst().getMetadata().getName().equals("Topic")),
            eq(TABLE), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldDisplayError() {
        CommandLine cmd = new CommandLine(apiResources);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());

        when(apiResourcesService.listResourceDefinitions())
            .thenThrow(exception);

        int code = cmd.execute();
        assertEquals(1, code);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }
}
