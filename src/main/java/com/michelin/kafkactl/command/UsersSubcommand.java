package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.service.FormatService.TABLE;
import static com.michelin.kafkactl.service.FormatService.YAML;

import com.michelin.kafkactl.hook.AuthenticatedCommand;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import picocli.CommandLine;

/**
 * Users subcommand.
 */
@CommandLine.Command(name = "reset-password",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Reset a Kafka password.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class UsersSubcommand extends AuthenticatedCommand {
    @Inject
    public ResourceService resourceService;

    @Inject
    public FormatService formatService;

    @CommandLine.Parameters(description = "The user to reset password.", arity = "1")
    public String user;

    @CommandLine.Option(names = {"--execute"}, description = "This option is mandatory to change the password")
    public boolean confirmed;

    @CommandLine.Option(names = {"-o",
        "--output"}, description = "Output format. One of: yaml|table", defaultValue = "table")
    public String output;

    /**
     * Run the "users" command.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws IOException {
        if (!List.of(TABLE, YAML).contains(output)) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                "Invalid value " + output + " for option -o.");
        }

        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());
        if (!confirmed) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                "You are about to change your Kafka password for the namespace " + namespace + ".\n"
                    + "Active connections will be killed instantly.\n\n"
                    + "To execute this operation, rerun the command with option --execute.");
        }

        return resourceService.resetPassword(namespace, user, output, commandSpec);
    }
}
