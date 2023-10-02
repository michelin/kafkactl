package com.michelin.kafkactl;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.services.FormatService.YAML;

import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;
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
public class UsersSubcommand implements Callable<Integer> {
    @Inject
    public LoginService loginService;

    @Inject
    public ResourceService resourceService;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @Inject
    public FormatService formatService;

    @CommandLine.Parameters(description = "The user to reset password.", arity = "1")
    public String user;

    @CommandLine.Option(names = {"--execute"}, description = "This option is mandatory to change the password")
    public boolean confirmed;

    @CommandLine.Option(names = {"-o",
        "--output"}, description = "Output format. One of: yaml|table", defaultValue = "table")
    public String output;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @Override
    public Integer call() throws Exception {
        if (!loginService.doAuthenticate(commandSpec, kafkactlCommand.verbose)) {
            return 1;
        }

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
