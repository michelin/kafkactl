package com.michelin.kafkactl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.services.ConfigService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ConnectClustersSubcommandTest {
    @Mock
    private KafkactlConfig kafkactlConfig;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ConfigService configService;

    @Mock
    private LoginService loginService;

    @Mock
    private KafkactlCommand kafkactlCommand;

    @InjectMocks
    private ConnectClustersSubcommand connectClustersSubcommand;

    @Test
    void shouldReturnInvalidCurrentContext() {
        CommandLine cmd = new CommandLine(connectClustersSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(false);

        int code = cmd.execute("vaults");
        assertEquals(1, code);
        assertTrue(sw.toString().contains("No valid current context found. "
            + "Use \"kafkactl config use-context\" to set a valid context."));
    }

    @Test
    void shouldNotExecuteWhenNotLoggedIn() {
        CommandLine cmd = new CommandLine(connectClustersSubcommand);
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean()))
            .thenReturn(false);

        int code = cmd.execute("vaults");
        assertEquals(1, code);
    }

    @Test
    void shouldListAvailableVaultsClustersSuccess() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
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
        when(configService.isCurrentContextValid())
            .thenReturn(true);
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
    void shouldDisplayErrorMessageWhenNoSecretsPassed() {
        when(configService.isCurrentContextValid())
            .thenReturn(true);
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
        when(configService.isCurrentContextValid())
            .thenReturn(true);
        when(loginService.doAuthenticate(any(), anyBoolean())).thenReturn(true);

        kafkactlCommand.optionalNamespace = Optional.empty();
        when(kafkactlConfig.getCurrentNamespace()).thenReturn("namespace");

        when(resourceService.vaultsOnConnectClusters(any(), any(), any(), any()))
            .thenReturn(0);

        CommandLine cmd = new CommandLine(connectClustersSubcommand);

        int code = cmd.execute("vaults", "connectCluster", "secret1", "secret2");
        assertEquals(0, code);
        verify(resourceService).vaultsOnConnectClusters("namespace", "connectCluster", List.of("secret1", "secret2"),
            cmd.getCommandSpec());
    }
}
