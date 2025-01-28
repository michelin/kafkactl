package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.ConfigService;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class GetTest {
    @Mock
    LoginService loginService;

    @Mock
    FormatService formatService;

    @Mock
    ApiResourcesService apiResourcesService;

    @Mock
    ResourceService resourceService;

    @Mock
    ConfigService configService;

    @Mock
    KafkactlConfig kafkactlConfig;

    @InjectMocks
    Get get;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(get);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(false);

        int code = cmd.execute("topics");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("No valid current context found. "
            + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotGetWhenNotAuthenticated() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(false);

        CommandLine cmd = new CommandLine(get);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topics");
        assertEquals(1, code);
    }

    @Test
    void shouldNotGetWhenUnknownOutput() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByName(any()))
            .thenReturn(Optional.of(apiResource));

        CommandLine cmd = new CommandLine(get);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topics", "-o", "unknownOutputFormat");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Invalid value unknownOutputFormat for option -o."));
    }

    @Test
    void shouldNotGetWhenServerNotHaveResourceType() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionByName(any()))
            .thenReturn(Optional.empty());

        CommandLine cmd = new CommandLine(get);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topics");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type topics."));
    }

    @Test
    void shouldNotGetWhenHttpClientThrowException() {
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

        when(apiResourcesService.getResourceDefinitionByName(any()))
            .thenReturn(Optional.of(apiResource));
        HttpClientResponseException e = new HttpClientResponseException("error", HttpResponse.serverError());
        when(resourceService.list(any(), any(), any(), any(), any(), any()))
            .thenThrow(e);

        CommandLine cmd = new CommandLine(get);

        int code = cmd.execute("topics", "myTopic", "-n", "namespace");
        assertEquals(1, code);
        verify(formatService).displayError(e, "Topic", "myTopic", cmd.getCommandSpec());
    }

    @Test
    void shouldNotGetWhenBadSearchFormat() {
        CommandLine cmd = new CommandLine(get);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topics", "myTopic", "--search=badFormat");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Value for option option '--search' (<String=String>) should be in KEY=VALUE[,KEY=VALUE]"));
        assertTrue(sw.toString().contains("but was badFormat"));
    }

    @Test
    void shouldGetOneResource() {
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

        when(apiResourcesService.getResourceDefinitionByName(any()))
            .thenReturn(Optional.of(apiResource));

        when(resourceService.list(any(), any(), any(), any(), any(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(get);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topics", "myTopic", "-n", "namespace");
        assertEquals(0, code);
        verify(resourceService)
            .list(Collections.singletonList(apiResource), "namespace",
                "myTopic", null, TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldGetAllTopics() {
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

        when(apiResourcesService.getResourceDefinitionByName(any()))
            .thenReturn(Optional.of(apiResource));
        when(resourceService.list(any(), any(), any(), any(), any(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(get);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topics", "-n", "namespace");
        assertEquals(0, code);
        verify(resourceService)
            .list(Collections.singletonList(apiResource), "namespace",
                "*", null, TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldGetAll() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
            .thenReturn("namespace");

        ApiResource nonNamespacedApiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(false)
            .synchronizable(true)
            .build();

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        when(apiResourcesService.listResourceDefinitions())
            .thenReturn(List.of(apiResource, nonNamespacedApiResource));
        when(resourceService.list(any(), any(), any(), any(), any(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(get);

        int code = cmd.execute("all");
        assertEquals(0, code);
        verify(resourceService)
            .list(Collections.singletonList(apiResource), "namespace",
                "*", null, TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldGetWithSearch() {
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

        when(apiResourcesService.getResourceDefinitionByName(any()))
            .thenReturn(Optional.of(apiResource));

        when(resourceService.list(any(), any(), any(), any(), any(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(get);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topics", "myTopic", "--search", "tag=test,policy=compact", "-n", "namespace");
        assertEquals(0, code);
        verify(resourceService)
            .list(Collections.singletonList(apiResource), "namespace",
                "myTopic", Map.of("tag", "test", "policy", "compact"), TABLE, cmd.getCommandSpec());
    }
}
