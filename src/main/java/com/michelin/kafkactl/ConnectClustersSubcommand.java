package com.michelin.kafkactl;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.parents.AuthenticatedCommand;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import java.util.List;
import lombok.AllArgsConstructor;
import picocli.CommandLine;

/**
 * Connect clusters subcommand.
 */
@CommandLine.Command(name = "connect-clusters",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Interact with connect clusters.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ConnectClustersSubcommand extends AuthenticatedCommand {
    /**
     * Gets or sets the sub command action.
     */
    @CommandLine.Parameters(index = "0", description = "Action to perform (${COMPLETION-CANDIDATES}).", arity = "1")
    public ConnectClustersAction action;

    /**
     * Gets or sets the Connect cluster name that will vault the secrets.
     */
    @CommandLine.Parameters(index = "1", defaultValue = "",
        description = "Connect cluster name that will vault the secrets.", arity = "1")
    public String connectCluster;

    /**
     * Gets or sets the list of secrets to vault.
     */
    @CommandLine.Parameters(index = "2..*", description = "Secrets to vaults separated by space.", arity = "0..*")
    public List<String> secrets;

    /**
     * Gets or sets the kafkactl configuration.
     */
    @Inject
    public KafkactlConfig kafkactlConfig;

    /**
     * Gets or sets the resource service.
     */
    @Inject
    public ResourceService resourceService;

    /**
     * Gets or sets the console format service.
     */
    @Inject
    public FormatService formatService;

    /**
     * Run the "connect-clusters" command.
     *
     * @return The command return code
     */
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

    /**
     * Connect clusters actions.
     */
    @AllArgsConstructor
    public enum ConnectClustersAction {
        /**
         * The vaults action.
         */
        VAULTS("vaults");

        /**
         * The action real name.
         */
        private final String name;

        /**
         * Override The to string method to display the action.
         *
         * @return The real action name.
         */
        @Override
        public String toString() {
            return name;
        }
    }
}
