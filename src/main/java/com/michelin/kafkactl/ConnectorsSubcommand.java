package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.michelin.kafkactl.services.FormatService.TABLE;

@CommandLine.Command(name = "connectors", description = "Interact with connectors (Pause/Resume/Restart)")
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
        if (!loginService.doAuthenticate()) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Login failed.");
        }

        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        if (connectors.stream().anyMatch(s -> s.equalsIgnoreCase("ALL"))) {
            ApiResource connectType = apiResourcesService.getResourceDefinitionFromKind("Connector")
                    .orElseThrow(() -> new CommandLine.ParameterException(commandSpec.commandLine(), "\"Connector\" kind not found."));
            connectors = resourceService.listResourcesWithType(connectType, namespace)
                    .stream()
                    .map(resource -> resource.getMetadata().getName())
                    .collect(Collectors.toList());
        }

        List<Resource> changeConnectorResponseList = connectors.stream()
                // Prepare request object
                .map(connector -> Resource.builder()
                        .metadata(ObjectMeta.builder()
                                .namespace(namespace)
                                .name(connector)
                                .build())
                        .spec(Map.of("action", action.toString()))
                        .build())
                .map(changeConnectorStateRequest -> resourceService.changeConnectorState(namespace, changeConnectorStateRequest.getMetadata().getName(), changeConnectorStateRequest))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!changeConnectorResponseList.isEmpty()) {
            formatService.displayList("ChangeConnectorState", changeConnectorResponseList, TABLE, commandSpec.commandLine().getOut());
            return 0;
        }

        commandSpec.commandLine().getErr().println("Cannot change state of given connectors.");
        return 1;
    }
}

enum ConnectorAction {
    PAUSE("pause"),
    RESUME("resume"),
    RESTART("restart");

    @Getter
    private final String realName;

    ConnectorAction(String realName) {
        this.realName = realName;
    }

    @Override
    public String toString() {
        return realName;
    }
}
