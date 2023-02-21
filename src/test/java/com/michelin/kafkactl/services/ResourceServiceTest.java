package com.michelin.kafkactl.services;

import com.michelin.kafkactl.KafkactlCommand;
import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.NamespacedResourceClient;
import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.models.SchemaCompatibility;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.*;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

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

        doCallRealMethod()
                .when(formatService).prettifyKind(any());
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
                .metadata(ObjectMeta.builder()
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
                .metadata(ObjectMeta.builder()
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
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.notFound());

        when(namespacedClient.apply(any(), any(), any(), any(), anyBoolean()))
                .thenThrow(exception);

        HttpResponse<Resource> actual = resourceService.apply(apiResource, "namespace", topicResource, false, cmd.getCommandSpec());

        assertNull(actual);
        verify(formatService).displayError(exception, "Topic", "prefix.topic", cmd.getCommandSpec());
    }

    @Test
    void shouldApplyNamespacedResource() {
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

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());
        when(namespacedClient.apply(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(HttpResponse
                        .ok(topicResource)
                        .header("X-Ns4kafka-Result", "created"));

        HttpResponse<Resource> actual = resourceService.apply(apiResource, "namespace", topicResource, false, cmd.getCommandSpec());

        assertEquals(HttpStatus.OK, actual.getStatus());
        assertEquals(topicResource, actual.body());
        assertTrue(sw.toString().contains("Topic \"prefix.topic\" created."));
    }

    @Test
    void shouldApplyNamespacedResourceNullHeaderInResponse() {
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

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());
        when(namespacedClient.apply(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(HttpResponse
                        .ok(topicResource));

        HttpResponse<Resource> actual = resourceService.apply(apiResource, "namespace", topicResource, false, cmd.getCommandSpec());

        assertEquals(HttpStatus.OK, actual.getStatus());
        assertEquals(topicResource, actual.body());
        assertTrue(sw.toString().contains("Topic \"prefix.topic\"."));
    }

    @Test
    void shouldApplyNonNamespacedResource() {
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

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());
        when(nonNamespacedClient.apply(any(), any(), any(), anyBoolean()))
                .thenReturn(HttpResponse
                        .ok(topicResource)
                        .header("X-Ns4kafka-Result", "created"));

        HttpResponse<Resource> actual = resourceService.apply(apiResource, "namespace", topicResource, false, cmd.getCommandSpec());

        assertEquals(HttpStatus.OK, actual.getStatus());
        assertEquals(topicResource, actual.body());
        assertTrue(sw.toString().contains("Topic \"prefix.topic\" created."));
    }

    @Test
    void shouldDeleteNamespacedResource() {
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

        doCallRealMethod().when(formatService).prettifyKind(any());
        when(namespacedClient.delete(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(HttpResponse
                        .<Void>ok()
                        .header("X-Ns4kafka-Result", "created"));

        boolean actual = resourceService.delete(apiResource, "namespace", "name", false, cmd.getCommandSpec());

        assertTrue(actual);
        assertTrue(sw.toString().contains("Topic \"name\" deleted."));
    }

    @Test
    void shouldDeleteNonNamespacedResource() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(false)
                .synchronizable(true)
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        doCallRealMethod().when(formatService).prettifyKind(any());
        when(nonNamespacedClient.delete(any(), any(), any(), anyBoolean()))
                .thenReturn(HttpResponse
                        .<Void>ok()
                        .header("X-Ns4kafka-Result", "created"));

        boolean actual = resourceService.delete(apiResource, "namespace", "name", false, cmd.getCommandSpec());

        assertTrue(actual);
        assertTrue(sw.toString().contains("Topic \"name\" deleted."));
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

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.delete(any(), any(), any(), any(), anyBoolean()))
                .thenThrow(exception);

        boolean actual = resourceService.delete(apiResource, "namespace", "prefix.topic", false, cmd.getCommandSpec());

        assertFalse(actual);
        verify(formatService).displayError(exception, "Topic", "prefix.topic", cmd.getCommandSpec());
    }

    @Test
    void shouldThrowExceptionWhenDeleteNotFoundResource() {
        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(namespacedClient.delete(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(HttpResponse.notFound());

        boolean actual = resourceService.delete(apiResource, "namespace", "prefix.topic", false, cmd.getCommandSpec());

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
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(namespacedClient.importResources(any(), any(), any(), anyBoolean()))
                .thenReturn(Collections.singletonList(topicResource));

        int actual = resourceService.importAll(Collections.singletonList(apiResource), "namespace", false,
                cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList("Topic", Collections.singletonList(topicResource), TABLE, cmd.getCommandSpec());
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

        CommandLine cmd = new CommandLine(new KafkactlCommand());
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

        CommandLine cmd = new CommandLine(new KafkactlCommand());

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
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(namespacedClient.deleteRecords(any(), any(), any(), anyBoolean()))
                .thenReturn(Collections.singletonList(topicResource));

        int actual = resourceService.deleteRecords("namespace", "topic", false, cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList(DELETE_RECORDS_RESPONSE, Collections.singletonList(topicResource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldDeleteRecordsEmpty() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());
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
        CommandLine cmd = new CommandLine(new KafkactlCommand());
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
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(namespacedClient.resetOffsets(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(Collections.singletonList(topicResource));

        int actual = resourceService.resetOffsets("namespace", "topic", topicResource, false, cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList(CONSUMER_GROUP_RESET_OFFSET_RESPONSE, Collections.singletonList(topicResource), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldResetOffsetsEmpty() {
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
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .creationTimestamp(Date.from(Instant.parse("2000-01-01T01:00:00.00Z")))
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());
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
                .metadata(ObjectMeta.builder()
                        .name("connector")
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(namespacedClient.changeConnectorState(any(), any(), any(), any()))
                .thenReturn(HttpResponse.ok(changeConnectorStateResource));

        Optional<Resource> actual = resourceService.changeConnectorState("namespace", "connector", changeConnectorStateResource, cmd.getCommandSpec());

        assertTrue(actual.isPresent());
        assertEquals(changeConnectorStateResource, actual.get());
    }

    @Test
    void shouldChangeConnectorStateNotFound() {
        Resource changeConnectorStateResource = Resource.builder()
                .kind(CHANGE_CONNECTOR_STATE)
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("connector")
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(namespacedClient.changeConnectorState(any(), any(), any(), any()))
                .thenReturn(HttpResponse.notFound(changeConnectorStateResource));

        Optional<Resource> actual = resourceService.changeConnectorState("namespace", "connector", changeConnectorStateResource,
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
                .metadata(ObjectMeta.builder()
                        .name("connector")
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.changeConnectorState(any(), any(), any(), any()))
                .thenThrow(exception);

        Optional<Resource> actual = resourceService.changeConnectorState("namespace", "connector", changeConnectorStateResource,
                cmd.getCommandSpec());

        assertTrue(actual.isEmpty());
        verify(formatService).displayError(exception, CONNECTOR, "connector", cmd.getCommandSpec());
    }

    @Test
    void shouldChangeSchemaCompatibility() {
        Resource changeSchemaCompatResource = Resource.builder()
                .kind(SCHEMA_COMPATIBILITY_STATE)
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("subject")
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(namespacedClient.changeSchemaCompatibility(any(), any(), any(), any()))
                .thenReturn(HttpResponse.ok(changeSchemaCompatResource));

        Optional<Resource> actual = resourceService.changeSchemaCompatibility("namespace", "subject", SchemaCompatibility.FORWARD_TRANSITIVE,
                cmd.getCommandSpec());

        assertTrue(actual.isPresent());
        assertEquals(changeSchemaCompatResource, actual.get());
    }

    @Test
    void shouldChangeSchemaCompatibilityNotFound() {
        Resource changeConnectorStateResource = Resource.builder()
                .kind(CHANGE_CONNECTOR_STATE)
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("subject")
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(namespacedClient.changeSchemaCompatibility(any(), any(), any(), any()))
                .thenReturn(HttpResponse.notFound(changeConnectorStateResource));

        Optional<Resource> actual = resourceService.changeSchemaCompatibility("namespace", "subject", SchemaCompatibility.FORWARD_TRANSITIVE,
                cmd.getCommandSpec());

        assertTrue(actual.isEmpty());
        verify(formatService).displayError(argThat(exception -> exception.getStatus().equals(HttpStatus.NOT_FOUND)
                        && exception.getMessage().equals("Not Found")), eq(SUBJECT), eq("subject"),
                eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldChangeSchemaCompatFail() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());

        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.changeSchemaCompatibility(any(), any(), any(), any()))
                .thenThrow(exception);

        Optional<Resource> actual = resourceService.changeSchemaCompatibility("namespace", "subject", SchemaCompatibility.FORWARD_TRANSITIVE,
                cmd.getCommandSpec());

        assertTrue(actual.isEmpty());
        verify(formatService).displayError(exception, SUBJECT, "subject", cmd.getCommandSpec());
    }

    @Test
    void shouldResetPassword() {
        Resource changeSchemaCompatResource = Resource.builder()
                .kind(KAFKA_USER_RESET_PASSWORD)
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("user")
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

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
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .build())
                .spec(Map.of())
                .build();

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(namespacedClient.resetPassword(any(), any(), any()))
                .thenReturn(HttpResponse.notFound(changeConnectorStateResource));

        int actual = resourceService.resetPassword("namespace", "username", TABLE, cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(argThat(exception -> exception.getStatus().equals(HttpStatus.NOT_FOUND)
                        && exception.getMessage().equals("Not Found")), eq(cmd.getCommandSpec()));
    }

    @Test
    void shouldResetPasswordFail() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());

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
                .metadata(ObjectMeta.builder()
                        .name("connectCluster")
                        .build())
                .spec(Map.of())
                .build();

        when(namespacedClient.listAvailableVaultsConnectClusters(any(), any()))
                .thenReturn(Collections.singletonList(availableVaults));

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        int actual = resourceService.listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList(CONNECT_CLUSTER, Collections.singletonList(availableVaults), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldListAvailableVaultsConnectClustersWhenEmpty() {
        when(namespacedClient.listAvailableVaultsConnectClusters(any(), any()))
                .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(new KafkactlCommand());
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

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        int actual = resourceService.listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldVaultsOnConnectClusters() {
        Resource availableVaults = Resource.builder()
                .kind(CONNECT_CLUSTER)
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("connectCluster")
                        .build())
                .spec(Map.of())
                .build();

        when(namespacedClient.vaultsOnConnectClusters(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(availableVaults));

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        int actual = resourceService.vaultsOnConnectClusters("namespace", "connectCluster", Collections.singletonList("passwd"),
                cmd.getCommandSpec());

        assertEquals(0, actual);
        verify(formatService).displayList(VAULT_RESPONSE, Collections.singletonList(availableVaults), TABLE, cmd.getCommandSpec());
    }

    @Test
    void shouldVaultsOnConnectClustersAndThrowException() {
        HttpClientResponseException exception = new HttpClientResponseException("error", HttpResponse.serverError());
        when(namespacedClient.listAvailableVaultsConnectClusters(any(), any()))
                .thenThrow(exception);

        CommandLine cmd = new CommandLine(new KafkactlCommand());

        int actual = resourceService.listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());

        assertEquals(1, actual);
        verify(formatService).displayError(exception, cmd.getCommandSpec());
    }

    @Test
    void shouldParse() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());

        doCallRealMethod()
                .when(fileService).computeYamlFileList(any(), anyBoolean());
        doCallRealMethod()
                .when(fileService).parseResourceListFromFiles(any());

        List<Resource> actual = resourceService.parseResources(Optional.of(new File("src/test/resources/topics/topic.yml")), false, cmd.getCommandSpec());

        assertEquals(1, actual.size());
        assertEquals("Topic", actual.get(0).getKind());
        assertEquals("myPrefix.topic", actual.get(0).getMetadata().getName());
        assertEquals(3, actual.get(0).getSpec().get("replicationFactor"));
        assertEquals(3, actual.get(0).getSpec().get("partitions"));
    }

    @Test
    void shouldNotParseNotFound() {
        CommandLine cmd = new CommandLine(new KafkactlCommand());

        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        CommandLine.ParameterException actual = assertThrows(CommandLine.ParameterException.class,
                () -> resourceService.parseResources(Optional.of(new File("src/test/resources/topics/topic.yml")), false, cmd.getCommandSpec()));

        assertEquals("Could not find YAML or YML files in topic.yml directory.", actual.getMessage());
    }
}
