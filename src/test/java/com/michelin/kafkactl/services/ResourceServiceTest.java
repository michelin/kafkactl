package com.michelin.kafkactl.services;

import com.michelin.kafkactl.KafkactlCommand;
import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.NamespacedResourceClient;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {
    @Mock
    NamespacedResourceClient namespacedClient;

    @Mock
    ClusterResourceClient nonNamespacedClient;

    @Mock
    LoginService loginService;

    @Mock
    FormatService formatService;

    @Mock
    FileService fileService;

    @InjectMocks
    ResourceService resourceService;

    @Test
    void shouldListAllWhenOneNamespacedApiResource() {
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
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(namespacedClient.list(any(), any(), any()))
                .thenReturn(Collections.singletonList(resource));

        int actual = resourceService.listAll(Collections.singletonList(apiResource), "namespace", cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList("Topic", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldListAllWhenOneNonNamespacedApiResource() {
        ApiResource apiResource = ApiResource.builder()
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
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(nonNamespacedClient.list(any(), any()))
                .thenReturn(Collections.singletonList(resource));

        int actual = resourceService.listAll(Collections.singletonList(apiResource), "namespace", cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList("Topic", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldListAllWhenEmptyResponse() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(namespacedClient.list(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        int actual = resourceService.listAll(Collections.singletonList(apiResource), "namespace", cmd.getCommandSpec());

        assertEquals(0, actual);
        assertTrue(sw.toString().contains("No topic to display."));
    }

    @Test
    void shouldNotListAllWhenHttpClientResponseException() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.list(any(), any(), any()))
                .thenThrow(exception);

        int actual = resourceService.listAll(Collections.singletonList(apiResource), "namespace", cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldListAllWhenMultipleApiResources() {
        ApiResource apiResourceOne = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        ApiResource apiResourceTwo = ApiResource.builder()
                .kind("Connector")
                .namespaced(true)
                .synchronizable(true)
                .path("connectors")
                .names(List.of("connects", "connect", "co"))
                .build();

        Resource topicResource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        Resource connectorResource = Resource.builder()
                .kind("Connector")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(namespacedClient.list(any(), any(), any()))
                .thenReturn(Collections.singletonList(topicResource))
                .thenReturn(Collections.singletonList(connectorResource));

        int actual = resourceService.listAll(List.of(apiResourceOne, apiResourceTwo), "namespace", cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList("Topic", Collections.singletonList(topicResource), TABLE, cmd.getCommandSpec());
        verify(formatService).displayList("Connector", Collections.singletonList(connectorResource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldListAllWhenMultipleApiResourcesAndException() {
        ApiResource apiResourceOne = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        ApiResource apiResourceTwo = ApiResource.builder()
                .kind("Connector")
                .namespaced(true)
                .synchronizable(true)
                .path("connectors")
                .names(List.of("connects", "connect", "co"))
                .build();

        Resource topicResource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.list(any(), any(), any()))
                .thenReturn(Collections.singletonList(topicResource))
                .thenThrow(exception);

        int actual = resourceService.listAll(List.of(apiResourceOne, apiResourceTwo), "namespace", cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayList("Topic", Collections.singletonList(topicResource), TABLE, cmd.getCommandSpec());
        verify(formatService).displayError(exception, "Connector", cmd.getCommandSpec());
    }

    @Test
    void shouldGetSingleNamespacedResourceWithType() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        Resource topicResource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        when(namespacedClient.get(any(), any(), any(), any()))
                .thenReturn(HttpResponse.ok(topicResource));

        Resource actual = resourceService.getSingleResourceWithType(apiResource, "namespace", "resourceName", false);

        assertEquals(topicResource, actual);
    }

    @Test
    void shouldGetSingleNonNamespacedResourceWithType() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(false)
                .synchronizable(true)
                .build();

        Resource topicResource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        when(nonNamespacedClient.get(any(), any(), any()))
                .thenReturn(HttpResponse.ok(topicResource));

        Resource actual = resourceService.getSingleResourceWithType(apiResource, "namespace", "resourceName", false);

        assertEquals(topicResource, actual);
    }

    @Test
    void shouldThrowExceptionWhenGetSingleResourceWithTypeNotFound() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.notFound());
        when(namespacedClient.get(any(), any(), any(), any()))
                .thenThrow(exception);

        HttpClientResponseException actual = assertThrows(HttpClientResponseException.class,
                () -> resourceService.getSingleResourceWithType(apiResource, "namespace",
                        "resourceName", true));

        assertEquals("error", actual.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, actual.getStatus());
    }
}
