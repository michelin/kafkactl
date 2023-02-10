package com.michelin.kafkactl;

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.NamespacedResourceClient;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSubcommandTest {
    @Mock
    public LoginService loginService;

    @Mock
    public FormatService formatService;

    @Mock
    public ApiResourcesService apiResourcesService;

    @Mock
    public ResourceService resourceService;

    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private GetSubcommand getSubcommand;

    @Test
    void shouldNotGetWhenNotAuthenticated() {
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(false);

        CommandLine cmd = new CommandLine(getSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topics");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("Login failed."));
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

        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionFromCommandName(any()))
                .thenReturn(Optional.of(apiResource));

        CommandLine cmd = new CommandLine(getSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topics", "-o", "unknownOutputFormat");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Invalid value unknownOutputFormat for option -o."));
    }

    @Test
    void shouldNotGetWhenServerNotHaveResourceType() {
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionFromCommandName(any()))
                .thenReturn(Optional.empty());

        CommandLine cmd = new CommandLine(getSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("topics");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type topics."));
    }

    @Test
    void shouldNotGetWhenHttpClientThrowException() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionFromCommandName(any()))
                .thenReturn(Optional.of(apiResource));
        HttpClientResponseException e = new HttpClientResponseException("error", HttpResponse.serverError());
        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenThrow(e);

        CommandLine cmd = new CommandLine(getSubcommand);

        int code = cmd.execute("topics", "myTopic");
        assertEquals(1, code);
        verify(formatService).displayError(e, "Topic", "myTopic", cmd.getCommandSpec());
    }

    @Test
    void shouldNotGetWhenThrowException() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionFromCommandName(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenThrow(new RuntimeException("error"));

        CommandLine cmd = new CommandLine(getSubcommand);

        int code = cmd.execute("topics", "myTopic");
        assertEquals(1, code);
        verify(formatService).displayError("Error getting resource type", "Topic", "myTopic", cmd.getCommandSpec());
    }

    @Test
    void shouldGetSingleResource() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionFromCommandName(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.getSingleResourceWithType(any(), any(), any(), anyBoolean()))
                .thenReturn(resource);

        CommandLine cmd = new CommandLine(getSubcommand);

        int code = cmd.execute("topics", "myTopic");
        assertEquals(0, code);
        verify(formatService).displaySingle(resource, TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldGetAllTopicsNoResourceToDisplay() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionFromCommandName(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.listAll(any(), any(), any()))
                .thenReturn(Collections.singletonMap(apiResource, Collections.emptyList()));

        CommandLine cmd = new CommandLine(getSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("topics");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("No resource to display."));
    }

    @Test
    void shouldGetAllTopics() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        kafkactlCommand.optionalNamespace = Optional.of("namespace");
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(apiResourcesService.getResourceDefinitionFromCommandName(any()))
                .thenReturn(Optional.of(apiResource));
        when(resourceService.listAll(any(), any(), any()))
                .thenReturn(Collections.singletonMap(apiResource, Collections.singletonList(resource)));

        CommandLine cmd = new CommandLine(getSubcommand);

        int code = cmd.execute("topics");
        assertEquals(0, code);
        verify(formatService).displayList("Topic", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldGetAll() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        ApiResource nonNamespacedApiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(false)
                .synchronizable(true)
                .build();

        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(List.of(apiResource, nonNamespacedApiResource));
        when(resourceService.listAll(any(), any(), any()))
                .thenReturn(Collections.singletonMap(apiResource, Collections.singletonList(resource)));

        CommandLine cmd = new CommandLine(getSubcommand);

        int code = cmd.execute("all");
        assertEquals(0, code);
        verify(formatService).displayList("Topic", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldNotGetAllWhenHttpClientThrowException() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(Collections.singletonList(apiResource));
        HttpClientResponseException e = new HttpClientResponseException("error", HttpResponse.serverError());
        when(resourceService.listAll(any(), any(), any()))
                .thenThrow(e);

        CommandLine cmd = new CommandLine(getSubcommand);

        int code = cmd.execute("all");
        assertEquals(1, code);
        verify(formatService).displayError(e, "Topic", null, cmd.getCommandSpec());
    }

    @Test
    void shouldNotGetAllWhenThrowException() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        ApiResource nonNamespacedApiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(false)
                .synchronizable(true)
                .build();

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(loginService.doAuthenticate(anyBoolean()))
                .thenReturn(true);
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(List.of(apiResource, nonNamespacedApiResource));
        when(resourceService.listAll(any(), any(), any()))
                .thenThrow(new RuntimeException("error"));

        CommandLine cmd = new CommandLine(getSubcommand);

        int code = cmd.execute("all");
        assertEquals(1, code);
        verify(formatService).displayError("Error getting resource type", "Topic", null, cmd.getCommandSpec());
    }
}
