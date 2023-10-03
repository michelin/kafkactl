package com.michelin.kafkactl;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.SCHEMA_COMPATIBILITY_STATE;

import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.models.SchemaCompatibility;
import com.michelin.kafkactl.parents.AuthenticatedCommand;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import picocli.CommandLine;

/**
 * Schema subcommand.
 */
@CommandLine.Command(name = "schemas",
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
public class SchemaSubcommand extends AuthenticatedCommand {
    @Inject
    public ResourceService resourceService;

    @Inject
    public FormatService formatService;

    @CommandLine.Parameters(index = "0", description = "Compatibility to set (${COMPLETION-CANDIDATES}).", arity = "1")
    public SchemaCompatibility compatibility;

    @CommandLine.Parameters(index = "1..*", description = "Subject names separated by space.", arity = "1..*")
    public List<String> subjects;

    /**
     * Run the "reset-offsets" command.
     *
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws Exception {
        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        List<Resource> updatedSchemas = subjects
            .stream()
            .map(subject -> resourceService.changeSchemaCompatibility(namespace, subject, compatibility, commandSpec))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        if (!updatedSchemas.isEmpty()) {
            formatService.displayList(SCHEMA_COMPATIBILITY_STATE, updatedSchemas, TABLE, commandSpec);
            return 0;
        }

        return 1;
    }
}
