package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ConfigService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.utils.VersionProvider;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine;

import java.util.*;
import java.util.concurrent.Callable;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.CONTEXT;

@CommandLine.Command(name = "config",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@:%n%n",
        description = "Manage configuration.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true,
        versionProvider = VersionProvider.class,
        mixinStandardHelpOptions = true)
public class ConfigSubcommand implements Callable<Integer> {
    @Inject
    public KafkactlConfig kafkactlConfig;

    @Inject
    public ConfigService configService;

    @Inject
    public FormatService formatService;

    @CommandLine.Parameters(index = "0", description = "Action to perform (${COMPLETION-CANDIDATES}).", arity = "1")
    public ConfigAction action;

    @CommandLine.Parameters(index="1", defaultValue = "", description = "Context to use.", arity = "1")
    public String context;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "config" command
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer call() throws Exception {
        if (action.equals(ConfigAction.CURRENT_CONTEXT)) {
            Map<String,Object> specs = new HashMap<>();

            if (kafkactlConfig.getCurrentNamespace() != null)
                specs.put("namespace", kafkactlConfig.getCurrentNamespace());

            if (kafkactlConfig.getApi() != null)
                specs.put("api", kafkactlConfig.getApi());

            if (kafkactlConfig.getUserToken() != null)
                specs.put("token", kafkactlConfig.getUserToken());

            String currentContextName = configService.getCurrentContextName();
            Resource currentContextAsResource = Resource.builder()
                    .metadata(ObjectMeta.builder()
                            .name(currentContextName != null ? currentContextName : StringUtils.EMPTY_STRING)
                            .build())
                    .spec(specs)
                    .build();

            formatService.displayList(CONTEXT, List.of(currentContextAsResource), TABLE, commandSpec);
            return 0;
        }

        if (kafkactlConfig.getContexts().isEmpty()) {
            commandSpec.commandLine().getOut().println("No context pre-defined.");
            return 0;
        }

        if (action.equals(ConfigAction.GET_CONTEXTS)) {
            List<Resource> allContextsAsResources = new ArrayList<>();
            kafkactlConfig.getContexts().forEach(userContext -> {
                Map<String,Object> specs = new HashMap<>();
                specs.put("namespace", userContext.getDefinition().getNamespace());
                specs.put("api", userContext.getDefinition().getApi());
                specs.put("token", userContext.getDefinition().getUserToken());

                Resource currentContextAsResource = Resource.builder()
                        .metadata(ObjectMeta.builder()
                                .name(userContext.getName())
                                .build())
                        .spec(specs)
                        .build();

                allContextsAsResources.add(currentContextAsResource);
            });

            formatService.displayList(CONTEXT, allContextsAsResources, TABLE, commandSpec);
            return 0;
        }

        Optional<KafkactlConfig.Context> optionalContextToSet = configService.getContextByName(context);
        if (optionalContextToSet.isEmpty()) {
            commandSpec.commandLine().getErr().println("No context exists with the name: " + context);
            return 1;
        }

        KafkactlConfig.Context contextToSet = optionalContextToSet.get();
        if (action.equals(ConfigAction.USE_CONTEXT)) {
            configService.updateConfigurationContext(contextToSet);
            commandSpec.commandLine().getOut().println("Switched to context \"" + context + "\".");
            return 0;
        }

        return 1;
    }
}

enum ConfigAction {
    GET_CONTEXTS("get-contexts"),
    CURRENT_CONTEXT("current-context"),
    USE_CONTEXT("use-context");

    @Getter
    private final String name;

    ConfigAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
