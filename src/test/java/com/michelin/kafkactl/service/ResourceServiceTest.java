package com.michelin.kafkactl.service;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CHANGE_CONNECTOR_STATE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECTOR;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECT_CLUSTER;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONSUMER_GROUP_RESET_OFFSET_RESPONSE;
import static com.michelin.kafkactl.util.constant.ResourceKind.DELETE_RECORDS_RESPONSE;
import static com.michelin.kafkactl.util.constant.ResourceKind.KAFKA_USER_RESET_PASSWORD;
import static com.michelin.kafkactl.util.constant.ResourceKind.SCHEMA_COMPATIBILITY_STATE;
import static com.michelin.kafkactl.util.constant.ResourceKind.SUBJECT;
import static com.michelin.kafkactl.util.constant.ResourceKind.VAULT_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.NamespacedResourceClient;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.model.SchemaCompatibility;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

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
    void shouldListNamespacedApiResource() {
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
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.list(any(), any(), any(), any()))
            .thenReturn(Collections.singletonList(resource));

        int actual = resourceService.list(
            Collections.singletonList(apiResource), "namespace", TABLE, cmd.getCommandSpec(), "*"
        );

        assertEquals(0, actual);
        verify(formatService).displayList("Topic", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldListNonNamespacedApiResource() {
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
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(nonNamespacedClient.list(any(), any(), any()))
            .thenReturn(Collections.singletonList(resource));

        int actual = resourceService.list(
            Collections.singletonList(apiResource), "namespace", TABLE, cmd.getCommandSpec(), "*"
        );

        assertEquals(0, actual);
        verify(formatService).displayList("Topic", Collections.singletonList(resource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldNotListApiResourceWhenEmptyResponse() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod()
            .when(formatService).prettifyKind(any());
        when(namespacedClient.list(any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        int actual = resourceService.list(
            Collections.singletonList(apiResource), "namespace", TABLE, cmd.getCommandSpec(), "*"
        );

        assertEquals(0, actual);
        assertTrue(sw.toString().contains("No topic to display."));
    }

    @Test
    void shouldNotListApiResourceWhenHttpClientResponseException() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.list(any(), any(), any(), any()))
            .thenThrow(exception);

        int actual = resourceService.list(
            Collections.singletonList(apiResource), "namespace", TABLE, cmd.getCommandSpec(), "*"
        );

        assertEquals(1, actual);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldListApiResourceWhenMultipleResourceKinds() {
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
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        Resource connectorResource = Resource.builder()
            .kind("Connector")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(namespacedClient.list(any(), any(), any(), any()))
            .thenReturn(Collections.singletonList(topicResource))
            .thenReturn(Collections.singletonList(connectorResource));

        int actual = resourceService.list(
            List.of(apiResourceOne, apiResourceTwo), "namespace", TABLE, cmd.getCommandSpec(), "*"
        );

        assertEquals(0, actual);
        verify(formatService).displayList("Topic", Collections.singletonList(topicResource), TABLE,
            cmd.getCommandSpec());
        verify(formatService).displayList("Connector", Collections.singletonList(connectorResource), TABLE,
            cmd.getCommandSpec());
    }

    @Test
    void shouldListApiResourcesWhenMultipleResourceKindsAndNoResource() {
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

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(namespacedClient.list(any(), any(), any(), any()))
            .thenReturn(List.of())
            .thenReturn(List.of());

        int actual = resourceService.list(
            List.of(apiResourceOne, apiResourceTwo), "namespace", TABLE, cmd.getCommandSpec(), "*"
        );

        assertEquals(0, actual);
        verify(formatService, never()).displayList(any(), any(), any(), any());
    }

    @Test
    void shouldListMultipleApiResourcesWhenException() {
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
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.list(any(), any(), any(), any()))
            .thenReturn(Collections.singletonList(topicResource))
            .thenThrow(exception);

        int actual = resourceService.list(
            List.of(apiResourceOne, apiResourceTwo), "namespace", TABLE, cmd.getCommandSpec(), "*"
        );

        assertEquals(1, actual);
        verify(formatService).displayList("Topic", Collections.singletonList(topicResource), TABLE,
            cmd.getCommandSpec());
        verify(formatService).displayError(exception, "Connector", "*", cmd.getCommandSpec());
    }

    @Test
    void shouldNotListApiResourceWhenNoResourceMatchesName() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        doCallRealMethod().when(formatService).prettifyKind(any());
        when(namespacedClient.list(any(), any(), any(), any())).thenReturn(List.of());

        int actual = resourceService.list(
            List.of(apiResource), "namespace", TABLE, cmd.getCommandSpec(), "*-test"
        );

        assertEquals(0, actual);
        verify(formatService, never()).displayList(any(), any(), any(), any());
    }

    @Test
    void shouldNotListApiResourceWhenResourceNotFound() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());
        when(namespacedClient.list(any(), any(), any(), any())).thenReturn(List.of());

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        int actual = resourceService.list(
            List.of(apiResource), "namespace", TABLE, cmd.getCommandSpec(), "*"
        );

        assertEquals(0, actual);
        verify(formatService, never()).displayList(any(), any(), any(), any());
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
            .metadata(Metadata.builder()
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
            .metadata(Metadata.builder()
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

        Resource topicResource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        when(namespacedClient.get(any(), any(), any(), any()))
            .thenReturn(HttpResponse.notFound(topicResource));

        HttpClientResponseException actual = assertThrows(HttpClientResponseException.class,
            () -> resourceService.getSingleResourceWithType(apiResource, "namespace",
                "resourceName", true));

        assertEquals("Not Found", actual.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, actual.getStatus());
    }

    @Test
    void shouldNotThrowExceptionWhenGetSingleResourceWithTypeNotFound() {
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
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        when(namespacedClient.get(any(), any(), any(), any()))
            .thenReturn(HttpResponse.notFound(topicResource));

        Resource actual = resourceService.getSingleResourceWithType(apiResource, "namespace",
            "resourceName", false);

        assertEquals(topicResource, actual);
    }

    @Test
    void shouldThrowServerExceptionWhenGetSingleResourceWithType() {
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
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        when(namespacedClient.get(any(), any(), any(), any()))
            .thenReturn(HttpResponse.serverError(topicResource));

        Resource actual = resourceService.getSingleResourceWithType(apiResource, "namespace",
            "resourceName", false);

        assertEquals(topicResource, actual);
    }

    @Test
    void shouldApplyNamespacedResourceAndHandleHttpResponseException() {
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
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.notFound());

        when(namespacedClient.apply(any(), any(), any(), any(), anyBoolean()))
            .thenThrow(exception);

        HttpResponse<Resource> actual =
            resourceService.apply(apiResource, "namespace", topicResource, false, cmd.getCommandSpec());

        assertNull(actual);
        verify(formatService).displayError(exception, "Topic", "prefix.topic", cmd.getCommandSpec());
    }

    @Test
    void shouldApplyNamespacedResource() {
        Resource topicResource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());
        when(namespacedClient.apply(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(HttpResponse
                .ok(topicResource)
                .header("X-Ns4kafka-Result", "created"));

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        HttpResponse<Resource> actual =
            resourceService.apply(apiResource, "namespace", topicResource, false, cmd.getCommandSpec());

        assertEquals(HttpStatus.OK, actual.getStatus());
        assertEquals(topicResource, actual.body());
        assertTrue(sw.toString().contains("Topic \"prefix.topic\" created."));
    }

    @Test
    void shouldApplyNamespacedResourceNullHeaderInResponse() {
        Resource topicResource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());
        when(namespacedClient.apply(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(HttpResponse
                .ok(topicResource));

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        HttpResponse<Resource> actual =
            resourceService.apply(apiResource, "namespace", topicResource, false, cmd.getCommandSpec());

        assertEquals(HttpStatus.OK, actual.getStatus());
        assertEquals(topicResource, actual.body());
        assertTrue(sw.toString().contains("Topic \"prefix.topic\"."));
    }

    @Test
    void shouldApplyNonNamespacedResource() {
        Resource topicResource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());
        when(nonNamespacedClient.apply(any(), any(), any(), anyBoolean()))
            .thenReturn(HttpResponse
                .ok(topicResource)
                .header("X-Ns4kafka-Result", "created"));

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(false)
            .synchronizable(true)
            .build();

        HttpResponse<Resource> actual =
            resourceService.apply(apiResource, "namespace", topicResource, false, cmd.getCommandSpec());

        assertEquals(HttpStatus.OK, actual.getStatus());
        assertEquals(topicResource, actual.body());
        assertTrue(sw.toString().contains("Topic \"prefix.topic\" created."));
    }

    @Test
    void shouldDeleteNamespacedResource() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());

        Resource deletedResource = Resource.builder()
            .metadata(Metadata.builder()
                .name("name")
                .build())
            .build();

        when(namespacedClient.delete(any(), any(), any(), any(), any(), anyBoolean()))
            .thenReturn(HttpResponse
                .ok(List.of(deletedResource))
                .header("X-Ns4kafka-Result", "created"));

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        boolean actual = resourceService.delete(apiResource, "namespace", "name", null, false, cmd.getCommandSpec());

        assertTrue(actual);
        assertTrue(sw.toString().contains("Topic \"name\" deleted."));
    }

    @Test
    void shouldDeleteMultipleNamespacedResources() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());

        Resource deletedResource1 = Resource.builder()
            .metadata(Metadata.builder()
                .name("name1")
                .build())
            .build();

        Resource deletedResource2 = Resource.builder()
            .metadata(Metadata.builder()
                .name("name2")
                .build())
            .build();

        when(namespacedClient.delete(any(), any(), any(), any(), any(), anyBoolean()))
            .thenReturn(HttpResponse
                .ok(List.of(deletedResource1, deletedResource2))
                .header("X-Ns4kafka-Result", "created"));

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        boolean actual = resourceService.delete(apiResource, "namespace", "name*", null, false, cmd.getCommandSpec());

        assertTrue(actual);
        assertTrue(sw.toString().contains("Topic \"name1\" deleted."));
        assertTrue(sw.toString().contains("Topic \"name2\" deleted."));
    }

    @Test
    void shouldDeleteNamespacedResourceWithVersion() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());

        Resource deletedResource = Resource.builder()
            .metadata(Metadata.builder()
                .name("name")
                .build())
            .build();

        when(namespacedClient.delete(any(), any(), any(), any(), any(), anyBoolean()))
            .thenReturn(HttpResponse
                .ok(List.of(deletedResource))
                .header("X-Ns4kafka-Result", "created"));

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        boolean actual = resourceService.delete(apiResource, "namespace", "name",
            "latest", false, cmd.getCommandSpec());

        assertTrue(actual);
        assertTrue(sw.toString().contains("Topic \"name\" version latest deleted."));
    }

    @Test
    void shouldDeleteNonNamespacedResource() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());

        Resource deletedResource = Resource.builder()
            .metadata(Metadata.builder()
                .name("name")
                .build())
            .build();

        when(nonNamespacedClient.delete(any(), any(), any(), anyBoolean()))
            .thenReturn(HttpResponse
                .ok(List.of(deletedResource))
                .header("X-Ns4kafka-Result", "created"));

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(false)
            .synchronizable(true)
            .build();

        boolean actual = resourceService.delete(apiResource, "namespace", "name", null, false, cmd.getCommandSpec());

        assertTrue(actual);
        assertTrue(sw.toString().contains("Topic \"name\" deleted."));
    }

    @Test
    void shouldDeleteMultipleNonNamespacedResources() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());

        Resource deletedResource1 = Resource.builder()
            .metadata(Metadata.builder()
                .name("name1")
                .build())
            .build();

        Resource deletedResource2 = Resource.builder()
            .metadata(Metadata.builder()
                .name("name2")
                .build())
            .build();

        when(nonNamespacedClient.delete(any(), any(), any(), anyBoolean()))
            .thenReturn(HttpResponse
                .ok(List.of(deletedResource1, deletedResource2))
                .header("X-Ns4kafka-Result", "created"));

        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(false)
            .synchronizable(true)
            .build();

        boolean actual = resourceService.delete(apiResource, "namespace", "name*", null, false, cmd.getCommandSpec());

        assertTrue(actual);
        assertTrue(sw.toString().contains("Topic \"name1\" deleted."));
        assertTrue(sw.toString().contains("Topic \"name2\" deleted."));
    }

    @Test
    void shouldDeleteNamespacedResourceAndHandleHttpResponseException() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.delete(any(), any(), any(), any(), any(), anyBoolean()))
            .thenThrow(exception);

        boolean actual = resourceService.delete(apiResource, "namespace", "prefix.topic", null,
            false, cmd.getCommandSpec());

        assertFalse(actual);
        verify(formatService).displayError(exception, "Topic", "prefix.topic", cmd.getCommandSpec());
    }

    @Test
    void shouldNotDeleteWhenNamespacedResourceNotFound() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.delete(any(), any(), any(), any(), any(), anyBoolean()))
            .thenReturn(HttpResponse.notFound());

        boolean actual = resourceService.delete(apiResource, "namespace", "prefix.topic", null,
            false, cmd.getCommandSpec());

        assertFalse(actual);
        verify(formatService).displayError(argThat(exception -> exception.getStatus().equals(HttpStatus.NOT_FOUND)
                && exception.getMessage().equals("Not Found")), eq("Topic"), eq("prefix.topic"),
            eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldNotDeleteWhenNonNamespacedResourceNotFound() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(false)
            .synchronizable(true)
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(nonNamespacedClient.delete(any(), any(), any(), anyBoolean()))
            .thenReturn(HttpResponse.notFound());

        boolean actual = resourceService.delete(apiResource, "namespace", "prefix.topic", null,
            false, cmd.getCommandSpec());

        assertFalse(actual);
        verify(formatService).displayError(argThat(exception -> exception.getStatus().equals(HttpStatus.NOT_FOUND)
                && exception.getMessage().equals("Not Found")), eq("Topic"), eq("prefix.topic"),
            eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldImportAllSuccess() {
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
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.importResources(any(), any(), any(), anyBoolean()))
            .thenReturn(Collections.singletonList(topicResource));

        int actual = resourceService.importAll(Collections.singletonList(apiResource), "namespace", false,
            cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList("Topic", Collections.singletonList(topicResource), TABLE,
            cmd.getCommandSpec());
    }

    @Test
    void shouldImportAllEmpty() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(namespacedClient.importResources(any(), any(), any(), anyBoolean()))
            .thenReturn(Collections.emptyList());

        int actual = resourceService.importAll(Collections.singletonList(apiResource), "namespace", false,
            cmd.getCommandSpec());

        assertEquals(0, actual);
        assertTrue(sw.toString().contains("No topic to import."));
    }

    @Test
    void shouldImportAllFail() {
        ApiResource apiResource = ApiResource.builder()
            .kind("Topic")
            .path("topics")
            .names(List.of("topics", "topic", "to"))
            .namespaced(true)
            .synchronizable(true)
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.importResources(any(), any(), any(), anyBoolean()))
            .thenThrow(exception);

        int actual = resourceService.importAll(Collections.singletonList(apiResource), "namespace", false,
            cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldDeleteRecordsSuccess() {
        Resource topicResource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.deleteRecords(any(), any(), any(), anyBoolean()))
            .thenReturn(Collections.singletonList(topicResource));

        int actual = resourceService.deleteRecords("namespace", "topic", false, cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList(DELETE_RECORDS_RESPONSE, Collections.singletonList(topicResource), TABLE,
            cmd.getCommandSpec());
    }

    @Test
    void shouldDeleteRecordsEmpty() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(namespacedClient.deleteRecords(any(), any(), any(), anyBoolean()))
            .thenReturn(Collections.emptyList());

        int actual = resourceService.deleteRecords("namespace", "topic", false, cmd.getCommandSpec());

        assertEquals(0, actual);
        assertTrue(sw.toString().contains("No record to delete."));
    }

    @Test
    void shouldDeleteRecordsFail() {
        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.deleteRecords(any(), any(), any(), anyBoolean()))
            .thenThrow(exception);

        int actual = resourceService.deleteRecords("namespace", "topic", false, cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldResetOffsetsSuccess() {
        Resource topicResource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.resetOffsets(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(Collections.singletonList(topicResource));

        int actual = resourceService.resetOffsets("namespace", "topic", topicResource, false, cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList(CONSUMER_GROUP_RESET_OFFSET_RESPONSE,
            Collections.singletonList(topicResource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldResetOffsetsEmpty() {
        Resource topicResource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        when(namespacedClient.resetOffsets(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(Collections.emptyList());

        int actual = resourceService.resetOffsets("namespace", "topic", topicResource, false, cmd.getCommandSpec());

        assertEquals(0, actual);
        assertTrue(sw.toString().contains("No offset to reset."));
    }

    @Test
    void shouldResetOffsetsFail() {
        Resource topicResource = Resource.builder()
            .kind("Topic")
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.resetOffsets(any(), any(), any(), any(), anyBoolean()))
            .thenThrow(exception);

        int actual = resourceService.resetOffsets("namespace", "topic", topicResource, false, cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldChangeConnectorState() {
        Resource changeConnectorStateResource = Resource.builder()
            .kind(CHANGE_CONNECTOR_STATE)
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("connector")
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.changeConnectorState(any(), any(), any(), any()))
            .thenReturn(HttpResponse.ok(changeConnectorStateResource));

        Optional<Resource> actual =
            resourceService.changeConnectorState("namespace", "connector", changeConnectorStateResource,
                cmd.getCommandSpec());

        assertTrue(actual.isPresent());
        assertEquals(changeConnectorStateResource, actual.get());
    }

    @Test
    void shouldChangeConnectorStateNotFound() {
        Resource changeConnectorStateResource = Resource.builder()
            .kind(CHANGE_CONNECTOR_STATE)
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("connector")
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.changeConnectorState(any(), any(), any(), any()))
            .thenReturn(HttpResponse.notFound(changeConnectorStateResource));

        Optional<Resource> actual =
            resourceService.changeConnectorState("namespace", "connector", changeConnectorStateResource,
                cmd.getCommandSpec());

        assertTrue(actual.isEmpty());
        verify(formatService).displayError(argThat(exception -> exception.getStatus().equals(HttpStatus.NOT_FOUND)
                && exception.getMessage().equals("Not Found")), eq(CONNECTOR), eq("connector"),
            eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldChangeConnectorStateFail() {
        Resource changeConnectorStateResource = Resource.builder()
            .kind(CHANGE_CONNECTOR_STATE)
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("connector")
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.changeConnectorState(any(), any(), any(), any()))
            .thenThrow(exception);

        Optional<Resource> actual =
            resourceService.changeConnectorState("namespace", "connector", changeConnectorStateResource,
                cmd.getCommandSpec());

        assertTrue(actual.isEmpty());
        verify(formatService).displayError(exception, CONNECTOR, "connector", cmd.getCommandSpec());
    }

    @Test
    void shouldChangeSchemaCompatibility() {
        Resource changeSchemaCompatResource = Resource.builder()
            .kind(SCHEMA_COMPATIBILITY_STATE)
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("subject")
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.changeSchemaCompatibility(any(), any(), any(), any()))
            .thenReturn(HttpResponse.ok(changeSchemaCompatResource));

        Optional<Resource> actual =
            resourceService.changeSchemaCompatibility("namespace", "subject", SchemaCompatibility.FORWARD_TRANSITIVE,
                cmd.getCommandSpec());

        assertTrue(actual.isPresent());
        assertEquals(changeSchemaCompatResource, actual.get());
    }

    @Test
    void shouldChangeSchemaCompatibilityNotFound() {
        Resource changeConnectorStateResource = Resource.builder()
            .kind(CHANGE_CONNECTOR_STATE)
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("subject")
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.changeSchemaCompatibility(any(), any(), any(), any()))
            .thenReturn(HttpResponse.notFound(changeConnectorStateResource));

        Optional<Resource> actual =
            resourceService.changeSchemaCompatibility("namespace", "subject", SchemaCompatibility.FORWARD_TRANSITIVE,
                cmd.getCommandSpec());

        assertTrue(actual.isEmpty());
        verify(formatService).displayError(argThat(exception -> exception.getStatus().equals(HttpStatus.NOT_FOUND)
                && exception.getMessage().equals("Not Found")), eq(SUBJECT), eq("subject"),
            eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldChangeSchemaCompatFail() {
        CommandLine cmd = new CommandLine(new Kafkactl());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.changeSchemaCompatibility(any(), any(), any(), any()))
            .thenThrow(exception);

        Optional<Resource> actual =
            resourceService.changeSchemaCompatibility("namespace", "subject", SchemaCompatibility.FORWARD_TRANSITIVE,
                cmd.getCommandSpec());

        assertTrue(actual.isEmpty());
        verify(formatService).displayError(exception, SUBJECT, "subject", cmd.getCommandSpec());
    }

    @Test
    void shouldResetPassword() {
        Resource changeSchemaCompatResource = Resource.builder()
            .kind(KAFKA_USER_RESET_PASSWORD)
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("user")
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.resetPassword(any(), any(), any()))
            .thenReturn(HttpResponse.ok(changeSchemaCompatResource));

        int actual = resourceService.resetPassword("namespace", "username", TABLE, cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displaySingle(changeSchemaCompatResource, TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldResetPasswordNotFound() {
        Resource changeConnectorStateResource = Resource.builder()
            .kind(KAFKA_USER_RESET_PASSWORD)
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("prefix.topic")
                .build())
            .spec(Map.of())
            .build();

        CommandLine cmd = new CommandLine(new Kafkactl());

        when(namespacedClient.resetPassword(any(), any(), any()))
            .thenReturn(HttpResponse.notFound(changeConnectorStateResource));

        int actual = resourceService.resetPassword("namespace", "username", TABLE, cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(argThat(exception -> exception.getStatus().equals(HttpStatus.NOT_FOUND)
            && exception.getMessage().equals("Not Found")), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldResetPasswordFail() {
        CommandLine cmd = new CommandLine(new Kafkactl());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.resetPassword(any(), any(), any()))
            .thenThrow(exception);

        int actual = resourceService.resetPassword("namespace", "username", TABLE, cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldListAvailableVaultsConnectClusters() {
        Resource availableVaults = Resource.builder()
            .kind(CONNECT_CLUSTER)
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("connectCluster")
                .build())
            .spec(Map.of())
            .build();

        when(namespacedClient.listAvailableVaultsConnectClusters(any(), any()))
            .thenReturn(Collections.singletonList(availableVaults));

        CommandLine cmd = new CommandLine(new Kafkactl());

        int actual = resourceService.listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList(CONNECT_CLUSTER, Collections.singletonList(availableVaults), TABLE,
            cmd.getCommandSpec());
    }

    @Test
    void shouldListAvailableVaultsConnectClustersWhenEmpty() {
        when(namespacedClient.listAvailableVaultsConnectClusters(any(), any()))
            .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(new Kafkactl());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int actual = resourceService.listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());

        assertEquals(0, actual);
        assertTrue(sw.toString().contains("No connect cluster configured as vault."));
    }

    @Test
    void shouldListAvailableVaultsConnectClustersAndThrowException() {
        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.listAvailableVaultsConnectClusters(any(), any()))
            .thenThrow(exception);

        CommandLine cmd = new CommandLine(new Kafkactl());

        int actual = resourceService.listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldVaultsOnConnectClusters() {
        Resource availableVaults = Resource.builder()
            .kind(CONNECT_CLUSTER)
            .apiVersion("v1")
            .metadata(Metadata.builder()
                .name("connectCluster")
                .build())
            .spec(Map.of())
            .build();

        when(namespacedClient.vaultsOnConnectClusters(any(), any(), any(), any()))
            .thenReturn(Collections.singletonList(availableVaults));

        CommandLine cmd = new CommandLine(new Kafkactl());

        int actual =
            resourceService.vaultsOnConnectClusters("namespace", "connectCluster", Collections.singletonList("passwd"),
                cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList(VAULT_RESPONSE, Collections.singletonList(availableVaults), TABLE,
            cmd.getCommandSpec());
    }

    @Test
    void shouldVaultsOnConnectClustersAndThrowException() {
        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.listAvailableVaultsConnectClusters(any(), any()))
            .thenThrow(exception);

        CommandLine cmd = new CommandLine(new Kafkactl());

        int actual = resourceService.listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldParse() {
        CommandLine cmd = new CommandLine(new Kafkactl());

        doCallRealMethod()
            .when(fileService).computeYamlFileList(any(), anyBoolean());
        doCallRealMethod()
            .when(fileService).parseResourceListFromFiles(any());

        List<Resource> actual =
            resourceService.parseResources(Optional.of(new File("src/test/resources/topics/topic.yml")), false,
                cmd.getCommandSpec());

        assertEquals(1, actual.size());
        assertEquals("Topic", actual.getFirst().getKind());
        assertEquals("myPrefix.topic", actual.getFirst().getMetadata().getName());
        assertEquals(3, actual.getFirst().getSpec().get("replicationFactor"));
        assertEquals(3, actual.getFirst().getSpec().get("partitions"));
    }

    @Test
    void shouldNotParseNotFound() {
        CommandLine cmd = new CommandLine(new Kafkactl());

        when(fileService.computeYamlFileList(any(), anyBoolean()))
            .thenReturn(Collections.emptyList());

        Optional<File> file = Optional.of(new File("src/test/resources/topics/topic.yml"));
        CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();
        ParameterException actual = assertThrows(ParameterException.class,
            () -> resourceService.parseResources(file, false, spec));

        assertEquals("Could not find YAML or YML files in topic.yml directory.", actual.getMessage());
    }
}
