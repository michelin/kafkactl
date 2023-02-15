package com.michelin.kafkactl;

import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.models.SchemaCompatibility;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.SCHEMA_COMPATIBILITY_STATE;

@CommandLine.Command(name = "schemas", description = "Interact with schemas.")
public class SchemaSubcommand implements Callable<Integer> {
    @Inject
    public LoginService loginService;

    @Inject
    public ResourceService resourceService;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @Inject
    public FormatService formatService;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @CommandLine.Parameters(index = "0",  description = "Compatibility to set (${COMPLETION-CANDIDATES}).", arity = "1")
    public SchemaCompatibility compatibility;

    @CommandLine.Parameters(index = "1..*", description = "Subject names separated by space.", arity = "1..*")
    public List<String> subjects;

    /**
     * Run the "reset-offsets" command
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer call() throws Exception {
        if (!loginService.doAuthenticate(commandSpec, kafkactlCommand.verbose)) {
            return 1;
        }

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
