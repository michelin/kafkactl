package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ConfigService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectClustersSubcommandTest {
    @Mock
    public KafkactlConfig kafkactlConfig;

    @Mock
    public ConfigService configService;

    @Mock
    public FormatService formatService;

    @Mock
    public ResourceService resourceService;

    @Mock
    public LoginService loginService;

    @Mock
    public KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ConnectClustersSubcommand connectClustersSubcommand;

    @Test
    void shouldNotExecuteWhenNotLoggedIn() {
        when(loginService.doAuthenticate()).thenReturn(false);
        CommandLine cmd = new CommandLine(connectClustersSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("vaults");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("Login failed."));
    }

    @Test
    void shouldDisplayEmptyVaultsListWhenNoConnectClusterAndNoAvailableClusters() {
        when(loginService.doAuthenticate()).thenReturn(true);

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");

        when(resourceService.listAvailableVaultsConnectClusters("namespace"))
                .thenReturn(List.of());

        CommandLine cmd = new CommandLine(connectClustersSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("vaults");
        assertEquals(0, code);
        verify(formatService).displayList(eq("ConnectCluster"), eq(List.of()), eq("table"));
    }

    @Test
    void shouldDisplayVaultsListWhenNoConnectCluster() {
        when(loginService.doAuthenticate()).thenReturn(true);

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");

        Resource resource = Resource.builder()
                .kind("ConnectCluster")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder().build())
                .spec(Collections.emptyMap())
                .build();

        when(resourceService.listAvailableVaultsConnectClusters("namespace"))
                .thenReturn(List.of(resource));

        CommandLine cmd = new CommandLine(connectClustersSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("vaults");
        assertEquals(0, code);
        verify(formatService).displayList(eq("ConnectCluster"), eq(List.of(resource)), eq("table"));
    }

    @Test
    void shouldLDisplayErrorMessageWhenNoSecretsPassed() {
        when(loginService.doAuthenticate()).thenReturn(true);

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");

        CommandLine cmd = new CommandLine(connectClustersSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("vaults", "connectCluster");
        assertEquals(2, code);
        assertTrue(sw.toString().contains("No secrets to encrypt."));
    }

    @Test
    void shouldDisplayVaultsResultsWhenConnectClusterAndSecretsDefined() {
        when(loginService.doAuthenticate()).thenReturn(true);

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");

        Resource resource1 = Resource.builder()
                .kind("VaultResponse")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder().build())
                .spec(Map.of("clearText", "secret1", "encrypted", "encrypted1"))
                .build();
        Resource resource2 = Resource.builder()
                .kind("VaultResponse")
                .apiVersion("v1")
                .metadata(ObjectMeta.builder().build())
                .spec(Map.of("clearText", "secret2", "encrypted", "encrypted2"))
                .build();

        when(resourceService.vaultsOnConnectClusters("namespace", "connectCluster", List.of("secret1", "secret2")))
                .thenReturn(List.of(resource1, resource2));

        CommandLine cmd = new CommandLine(connectClustersSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int code = cmd.execute("vaults", "connectCluster", "secret1", "secret2");
        assertEquals(0, code);
        verify(formatService).displayList(eq("VaultResponse"), eq(List.of(resource1, resource2)), eq("table"));
    }
}