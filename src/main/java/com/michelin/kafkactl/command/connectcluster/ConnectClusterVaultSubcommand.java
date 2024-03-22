package com.michelin.kafkactl.command.connectcluster;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.service.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import java.util.List;
import picocli.CommandLine;

/**
 * Connect cluster vault subcommand.
 */
@CommandLine.Command(name = "vaults",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Vault secrets for a connect cluster.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ConnectClusterVaultSubcommand extends AuthenticatedHook {
    /**
     * Gets or sets the Connect cluster name that will vault the secrets.
     */
    @CommandLine.Parameters(index = "0", defaultValue = "",
        description = "Connect cluster name that will vault the secrets.", arity = "1")
    public String connectCluster;

    /**
     * Gets or sets the list of secrets to vault.
     */
    @CommandLine.Parameters(index = "1..*", description = "Secrets to vaults separated by space.", arity = "0..*")
    public List<String> secrets;

    /**
     * Gets or sets the resource service.
     */
    @Inject
    public ResourceService resourceService;

    @Override
    public Integer onAuthSuccess() {
        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        // if no parameters, list the available connect cluster to vault secrets
        if (connectCluster.isEmpty()) {
            return resourceService.listAvailableVaultsConnectClusters(namespace, commandSpec);
        }

        // if connect cluster define but no secrets to encrypt => show error no secrets to encrypt.
        if (secrets == null) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "No secrets to encrypt.");
        }

        // if connect cluster and secrets define.
        return resourceService.vaultsOnConnectClusters(namespace, connectCluster, secrets, commandSpec);
    }
}
