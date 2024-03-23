package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CHANGE_CONNECTOR_STATE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECTOR;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import com.michelin.kafkactl.util.VersionProvider;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class Connector extends AuthenticatedHook {
    @Inject
    private ResourceService resourceService;

    @Inject
    private FormatService formatService;

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
        String namespace = Kafkactl.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        try {
            if (connectors.stream().anyMatch(s -> s.equalsIgnoreCase("ALL"))) {
                ApiResource connectType = apiResourcesService.getResourceDefinitionByKind(CONNECTOR)
                    .orElseThrow(() -> new CommandLine.ParameterException(commandSpec.commandLine(),
                        "The server does not have resource type Connector."));

                connectors = resourceService.listResourcesWithType(connectType, namespace)
                    .stream()
                    .map(resource -> resource.getMetadata().getName())
                    .toList();
            }

            List<Resource> changeConnectorResponses = connectors.stream()
                .map(connector -> Resource.builder()
                    .metadata(Metadata.builder()
                        .namespace(namespace)
                        .name(connector)
                        .build())
                    .spec(Map.of("action", action.toString()))
                    .build())
                .map(changeConnectorStateRequest -> resourceService.changeConnectorState(namespace,
                    changeConnectorStateRequest.getMetadata().getName(), changeConnectorStateRequest, commandSpec))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

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
