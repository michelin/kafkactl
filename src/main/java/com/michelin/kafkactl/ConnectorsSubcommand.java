package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.CHANGE_CONNECTOR_STATE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.CONNECTOR;

@CommandLine.Command(name = "connectors", description = "Interact with connectors.")
public class ConnectorsSubcommand implements Callable<Integer> {
    @Inject
    public LoginService loginService;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @Inject
    public ResourceService resourceService;

    @Inject
    public ApiResourcesService apiResourcesService;

    @Inject
    public FormatService formatService;

    @CommandLine.Parameters(index = "0", description = "Action to perform (${COMPLETION-CANDIDATES}).", arity = "1")
    public ConnectorAction action;

    @CommandLine.Parameters(index="1..*", description = "Connector names separated by space or \"all\" for all connectors.", arity = "1..*")
    public List<String> connectors;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "connectors" command
     *
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer call() throws Exception {
        if (!loginService.doAuthenticate(kafkactlCommand.verbose)) {
            return 1;
        }

        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        try {
            if (connectors.stream().anyMatch(s -> s.equalsIgnoreCase("ALL"))) {
                ApiResource connectType = apiResourcesService.getResourceDefinitionByKind(CONNECTOR)
                        .orElseThrow(() -> new CommandLine.ParameterException(commandSpec.commandLine(), "The server does not have resource type Connector."));
                connectors = resourceService.listResourcesWithType(connectType, namespace, commandSpec)
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
}

enum ConnectorAction {
    PAUSE("pause"),
    RESUME("resume"),
    RESTART("restart");

    @Getter
    private final String name;

    ConnectorAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
