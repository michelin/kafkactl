package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectClustersSubcommandTest {
    @Mock
    private KafkactlConfig kafkactlConfig;

    @Mock
    private ResourceService resourceService;

    @Mock
    private LoginService loginService;

    @Mock
    private KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ConnectClustersSubcommand connectClustersSubcommand;

    @Test
    void shouldNotExecuteWhenNotLoggedIn() {
        CommandLine cmd = new CommandLine(connectClustersSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(false);

        int code = cmd.execute("vaults");
        assertEquals(1, code);
    }

    @Test
    void shouldListAvailableVaultsClustersSuccess() {
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");
        when(resourceService.listAvailableVaultsConnectClusters(any(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(connectClustersSubcommand);

        int code = cmd.execute("vaults");
        assertEquals(0, code);
        verify(resourceService).listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());
    }

    @Test
    void shouldListAvailableVaultsClustersFail() {
        when(loginService.doAuthenticate(any(), anyBoolean()))
                .thenReturn(true);

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");
        when(resourceService.listAvailableVaultsConnectClusters(any(), any()))
                .thenReturn(1);

        CommandLine cmd = new CommandLine(connectClustersSubcommand);

        int code = cmd.execute("vaults");
        assertEquals(1, code);
        verify(resourceService).listAvailableVaultsConnectClusters("namespace", cmd.getCommandSpec());
    }

    @Test
    void shouldLDisplayErrorMessageWhenNoSecretsPassed() {
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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
    void shouldVaultSuccess() {
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

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

        when(resourceService.vaultsOnConnectClusters(any(), any(), any(), any()))
                .thenReturn(0);

        CommandLine cmd = new CommandLine(connectClustersSubcommand);

        int code = cmd.execute("vaults", "connectCluster", "secret1", "secret2");
        assertEquals(0, code);
        verify(resourceService).vaultsOnConnectClusters("namespace", "connectCluster", List.of("secret1", "secret2"), cmd.getCommandSpec());
    }
}
