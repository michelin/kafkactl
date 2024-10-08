package com.michelin.kafkactl.command.connectcluster;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/**
 * Connect cluster vault subcommand.
 */
@Command(name = "vault",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Vault secrets for a connect cluster.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true)
public class ConnectClusterVault extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Parameters(index = "0", defaultValue = "",
        description = "Connect cluster name that will vault the secrets.", arity = "1")
    public String connectClusterName;

    @Parameters(index = "1..*", description = "Secrets to vaults separated by space.", arity = "0..*")
    public List<String> secrets;

    @Override
    public Integer onAuthSuccess() {
        String namespace = getNamespace();

        // if no parameters, list the available connect cluster to vault secrets
        if (connectClusterName.isEmpty()) {
            return resourceService.listAvailableVaultsConnectClusters(namespace, commandSpec);
        }

        // if connect cluster define but no secrets to encrypt => show error no secrets to encrypt.
        if (secrets == null) {
            throw new ParameterException(commandSpec.commandLine(), "No secrets to encrypt.");
        }

        // if connect cluster and secrets define.
        return resourceService.vaultsOnConnectClusters(namespace, connectClusterName, secrets, commandSpec);
    }
}
