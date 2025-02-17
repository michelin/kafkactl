package com.michelin.kafkactl.command.config;

import static com.michelin.kafkactl.mixin.UnmaskTokenMixin.MASKED;
import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONTEXT;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.hook.ValidCurrentContextHook;
import com.michelin.kafkactl.mixin.UnmaskTokenMixin;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Config current context subcommand.
 */
@Command(name = "current-context",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Get the current context.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true)
public class ConfigCurrentContext extends ValidCurrentContextHook {
    @Inject
    @ReflectiveAccess
    private KafkactlConfig kafkactlConfig;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Mixin
    public UnmaskTokenMixin unmaskTokenMixin;

    @Override
    public Integer onContextValid() {
        Map<String, Object> specs = new HashMap<>();
        specs.put("namespace", kafkactlConfig.getCurrentNamespace());
        specs.put("api", kafkactlConfig.getApi());

        if (unmaskTokenMixin.unmaskTokens) {
            specs.put("token", kafkactlConfig.getUserToken());
        } else {
            specs.put("token", MASKED);
        }

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
