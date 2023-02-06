package com.michelin.kafkactl;

import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Represents the Kafka Connect Cluster sub command class.
 */
@CommandLine.Command(name = "connect-clusters", description = "Interact with connect clusters (Vaults)")
public class ConnectClustersSubcommand implements Callable<Integer> {

    /**
     * Gets or sets the kafkactl parent command.
     */
    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    /**
     * Gets or sets the sub command action.
     */
    @CommandLine.Parameters(index = "0", description = "vaults", arity = "1")
    public ConnectClustersAction action;

    /**
     * Gets or sets the connect cluster name that will vault the secrets.
     */
    @CommandLine.Parameters(index = "1", defaultValue = "", description = "Connect Cluster name that will vault the secrets", arity = "1")
    public String connectCluster;

    /**
     * Gets or sets the list of secrets to vault.
     */
    @CommandLine.Parameters(index = "2..*", description = "Secrets to vaults separated by space", arity = "0..*")
    public List<String> secrets;

    /**
     * Gets or sets the login service.
     */
    @Inject
    public LoginService loginService;

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
     * Gets or sets the api resources service.
     */
    @Inject
    public ApiResourcesService apiResourcesService;

    /**
     * Gets or sets the console format service.
     */
    @Inject
    public FormatService formatService;

    /**
     * Gets or sets the command specification.
     */
    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "connect-clusters" command
     *
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer call() throws Exception {
        boolean authenticated = loginService.doAuthenticate();
        if (!authenticated) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Login failed");
        }

        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        // if no parameters, list the available connect cluster to vault secrets
        if (connectCluster.isEmpty()) {
            List<Resource> availableConnectClusters = resourceService.listAvailableVaultsConnectClusters(namespace);
            formatService.displayList("ConnectCluster", availableConnectClusters, "table");
            return 0;
        }

        // if connect cluster define but no secrets to encrypt => show error no secrets to encrypt.
        if (secrets == null || secrets.isEmpty()) {
            System.out.println("No secrets to encrypt.");
            return 1;
        }

        // if connect cluster and secrets define.
        List<Resource> results = resourceService.vaultsOnConnectClusters(namespace, connectCluster, secrets);
        formatService.displayList("VaultResponse", results, "table");
        return 0;
    }
}

/**
 * Represents the connect clusters available actions.
 */
enum ConnectClustersAction {
    /**
     * The vaults action.
     */
    VAULTS("vaults");

    /**
     * The action real name.
     */
    @Getter
    private final String realName;

    /**
     * Initializes a new value of the {@link ConnectClustersAction} enum.
     *
     * @param realName The action real name.
     */
    ConnectClustersAction(String realName) {
        this.realName = realName;
    }

    /**
     * Override The to string method to display the action.
     *
     * @return The real action name.
     */
    @Override
    public String toString() {
        return realName;
    }
}
