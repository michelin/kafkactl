package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FileService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @InjectMocks
    private ApplySubcommand applySubcommand;

    @Test
    void shouldNotApplyWhenNotAuthenticated() {
        when(loginService.doAuthenticate())
                .thenReturn(false);

        int code = new CommandLine(applySubcommand).execute();
        assertEquals(2, code);
    }

    @Test
    void shouldNotApplyWhenNoFileInStdin() {
        when(loginService.doAuthenticate())
                .thenReturn(true);

        int code = new CommandLine(applySubcommand).execute();
        assertEquals(2, code);
    }

    @Test
    void shouldNotApplyWhenYmlFileNotFound() {
        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(fileService.computeYamlFileList(any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        int code = new CommandLine(applySubcommand).execute("-f", "topic.yml");
        assertEquals(2, code);
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

        int code = new CommandLine(applySubcommand).execute("-f", "topic.yml");
        assertEquals(2, code);
    }
}
