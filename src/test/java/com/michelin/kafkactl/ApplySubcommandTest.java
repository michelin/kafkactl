package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FileService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import io.micronaut.http.HttpResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplySubcommandTest {
    @Mock
    public LoginService loginService;

    @Mock
    public ApiResourcesService apiResourcesService;

    @Mock
    public FileService fileService;

    @Mock
    public ResourceService resourceService;

    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ApplySubcommand applySubcommand;

    @Test
    void shouldNotApplyWhenNotAuthenticated() {
        when(loginService.doAuthenticate())
                .thenReturn(false);

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Login failed."));
    }

    @Test
    void shouldNotApplyWhenNoFileInStdin() {
        when(loginService.doAuthenticate())
                .thenReturn(true);

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Required one of -f or stdin."));
    }

    @Test
    void shouldNotApplyWhenYmlFileNotFound() {
        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Could not find YAML or YML files in topic directory."));
    }

    @Test
    void shouldNotApplyWhenInvalidResources() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.singletonList(resource));

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("The server does not have resource type(s) Topic."));
    }

    @Test
    void shouldNotApplyWhenNamespaceMismatch() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        KafkactlCommand parentCmd = new KafkactlCommand();
        parentCmd.optionalNamespace = Optional.of("namespaceMismatch");
        applySubcommand.kafkactlCommand = parentCmd;

        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Namespace mismatch between Kafkactl and YAML document Topic/prefix.topic."));
    }

    @Test
    void shouldNotApplyNullResponse() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        KafkactlCommand parentCmd = new KafkactlCommand();
        parentCmd.optionalNamespace = Optional.empty();
        applySubcommand.kafkactlCommand = parentCmd;

        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(Collections.singletonList(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean()))
                .thenReturn(null);

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("Cannot handle Ns4Kafka response."));
    }

    @Test
    void shouldApply() {
        Resource resource = Resource.builder()
                .kind("Topic")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder()
                        .name("prefix.topic")
                        .namespace("namespace")
                        .build())
                .spec(Collections.emptyMap())
                .build();

        ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        KafkactlCommand parentCmd = new KafkactlCommand();
        parentCmd.optionalNamespace = Optional.empty();
        applySubcommand.kafkactlCommand = parentCmd;

        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.singletonList(new File("path")));
        when(fileService.parseResourceListFromFiles(any()))
                .thenReturn(Collections.singletonList(resource));
        when(apiResourcesService.validateResourceTypes(any()))
                .thenReturn(Collections.emptyList());
        when(kafkactlConfig.getCurrentNamespace())
                .thenReturn("namespace");
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(Collections.singletonList(apiResource));
        when(resourceService.apply(any(), any(), any(), anyBoolean()))
                .thenReturn(HttpResponse
                        .ok(resource)
                        .header("X-Ns4kafka-Result", "Created"));

        CommandLine cmd = new CommandLine(applySubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("-f", "topic.yml");
        assertEquals(0, code);
        assertTrue(sw.toString().contains("Success Topic/prefix.topic (Created)."));
    }
}
