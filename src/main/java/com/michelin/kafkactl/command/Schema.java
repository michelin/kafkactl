package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.SCHEMA_COMPATIBILITY_STATE;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.model.SchemaCompatibility;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import com.michelin.kafkactl.util.VersionProvider;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Schema subcommand.
 */
@Command(name = "schemas",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Interact with schemas.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class Schema extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Parameters(index = "0", description = "Compatibility to set (${COMPLETION-CANDIDATES}).", arity = "1")
    public SchemaCompatibility compatibility;

    @Parameters(index = "1..*", description = "Subject names separated by space.", arity = "1..*")
    public List<String> subjects;

    /**
     * Run the "reset-offsets" command.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws IOException {
        String namespace = Kafkactl.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        List<Resource> updatedSchemas = subjects
            .stream()
            .map(subject -> resourceService.changeSchemaCompatibility(namespace, subject, compatibility, commandSpec))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

        if (!updatedSchemas.isEmpty()) {
            formatService.displayList(SCHEMA_COMPATIBILITY_STATE, updatedSchemas, TABLE, commandSpec);
            return 0;
        }

        return 1;
    }
}
