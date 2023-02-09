package com.michelin.kafkactl;

import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.NamespacedResourceClient;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSubcommandTest {
    @Mock
    public NamespacedResourceClient namespacedClient;

    @Mock
    public ClusterResourceClient nonNamespacedClient;

    @Mock
    public LoginService loginService;

    @Mock
    public ApiResourcesService apiResourcesService;

    @Mock
    public ResourceService resourceService;

    @Mock
    public FormatService formatService;

    @Mock
    public KafkactlConfig kafkactlConfig;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private DiffSubcommand getSubcommand;

    @Test
    void shouldNotGetWhenNotAuthenticated() {
        when(loginService.doAuthenticate())
                .thenReturn(false);

        CommandLine cmd = new CommandLine(getSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute();
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Login failed."));
    }
}
