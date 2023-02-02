package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.LoginService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiResourcesSubcommandTest {
    @Mock
    private ApiResourcesService apiResourcesService;

    @Mock
    private LoginService loginService;

    @InjectMocks
    private ApiResourcesSubcommand apiResourcesSubcommand;

    @Test
    void shouldDisplayApiResources() {
      ApiResource apiResource = ApiResource.builder()
                .kind("Topic")
                .path("topics")
                .names(List.of("topics", "topic", "to"))
                .namespaced(true)
                .synchronizable(true)
                .build();

        when(loginService.doAuthenticate())
                .thenReturn(true);
        when(apiResourcesService.getListResourceDefinition())
                .thenReturn(Collections.singletonList(apiResource));

        CommandLine cmd = new CommandLine(apiResourcesSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(0, code);
        assertEquals("KIND                            NAMES                         NAMESPACED\r\n" +
                "Topic                           topics,topic,to               true\r\n\r\n", sw.toString());
    }

    @Test
    void shouldNotDisplayApiResourcesWhenNotAuthenticated() {
        when(loginService.doAuthenticate())
                .thenReturn(false);

        CommandLine cmd = new CommandLine(apiResourcesSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Login failed."));
    }
}
