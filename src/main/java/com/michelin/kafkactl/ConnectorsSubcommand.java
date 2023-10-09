package com.michelin.kafkactl;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.CHANGE_CONNECTOR_STATE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.CONNECTOR;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.parents.AuthenticatedCommand;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import picocli.CommandLine;

/**
 * Connectors subcommand.
 */
@CommandLine.Command(name = "connectors",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Interact with connectors.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ConnectorsSubcommand extends AuthenticatedCommand {
    @Inject
    public ResourceService resourceService;

    @Inject
    public FormatService formatService;

    @CommandLine.Parameters(index = "0", description = "Action to perform (${COMPLETION-CANDIDATES}).", arity = "1")
    public ConnectorAction action;

    @CommandLine.Parameters(index = "1..*",
        description = "Connector names separated by space or \"all\" for all connectors.", arity = "1..*")
    public List<String> connectors;

    /**
     * Run the "connectors" command.
     *
     * @return The command return code
     */
    @Override
    public Integer onAuthSuccess() {
        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        try {
            if (connectors.stream().anyMatch(s -> s.equalsIgnoreCase("ALL"))) {
                ApiResource connectType = apiResourcesService.getResourceDefinitionByKind(CONNECTOR)
                    .orElseThrow(() -> new CommandLine.ParameterException(commandSpec.commandLine(),
                        "The server does not have resource type Connector."));
                connectors = resourceService.listResourcesWithType(connectType, namespace)
                    .stream()
                    .map(resource -> resource.getMetadata().getName())
                    .collect(Collectors.toList());
            }

            List<Resource> changeConnectorResponses = connectors.stream()
                // Prepare request object
                .map(connector -> Resource.builder()
                    .metadata(ObjectMeta.builder()
                        .namespace(namespace)
                        .name(connector)
                        .build())
                    .spec(Map.of("action", action.toString()))
                    .build())
                .map(changeConnectorStateRequest -> resourceService.changeConnectorState(namespace,
                    changeConnectorStateRequest.getMetadata().getName(), changeConnectorStateRequest, commandSpec))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

            if (!changeConnectorResponses.isEmpty()) {
                formatService.displayList(CHANGE_CONNECTOR_STATE, changeConnectorResponses, TABLE, commandSpec);
                return 0;
            }

            return 1;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }

    /**
     * Connector actions.
     */
    @Getter
    @AllArgsConstructor
    public enum ConnectorAction {
        PAUSE("pause"),
        RESUME("resume"),
        RESTART("restart");

        private final String name;

        @Override
        public String toString() {
            return name;
        }
    }
}
