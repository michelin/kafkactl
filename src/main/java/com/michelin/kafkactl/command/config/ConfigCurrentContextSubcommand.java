package com.michelin.kafkactl.command.config;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.CONTEXT;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.hook.ValidCurrentContextHook;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.utils.VersionProvider;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import picocli.CommandLine;

/**
 * Config current context subcommand.
 */
@CommandLine.Command(name = "current-context",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Get the current context.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ConfigCurrentContextSubcommand extends ValidCurrentContextHook {
    @Inject
    public KafkactlConfig kafkactlConfig;

    @Inject
    public FormatService formatService;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() throws Exception {
        super.call();

        Map<String, Object> specs = new HashMap<>();
        specs.put("namespace", kafkactlConfig.getCurrentNamespace());
        specs.put("api", kafkactlConfig.getApi());
        specs.put("token", kafkactlConfig.getUserToken());

        String currentContextName = configService.getCurrentContextName();
        Resource currentContextAsResource = Resource.builder()
            .metadata(Metadata.builder()
                .name(currentContextName != null ? currentContextName : StringUtils.EMPTY_STRING)
                .build())
            .spec(specs)
            .build();

        formatService.displayList(CONTEXT, List.of(currentContextAsResource), TABLE, commandSpec);
        return 0;
    }
}
