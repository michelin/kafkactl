package com.michelin.kafkactl.command;

import static com.michelin.kafkactl.service.FormatService.Output;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/**
 * Users subcommand.
 */
@Command(name = "reset-password",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Reset a Kafka password.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true)
public class ResetPassword extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Parameters(
        description = "The user to reset password.",
        arity = "1")
    public String user;

    @Option(
        names = {"--execute"},
        description = "This option is mandatory to change the password")
    public boolean confirmed;

    @Option(
        names = {"-o", "--output"},
        description = "Output format. One of: yml|yaml|table",
        defaultValue = "table")
    public Output output;

    /**
     * Run the "users" command.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws IOException {
        String namespace = getNamespace();
        if (!confirmed) {
            commandSpec.commandLine().getOut().println("You are about to change your Kafka password "
                + "for the namespace " + namespace + ".\n"
                + "Active connections will be killed instantly.\n\n"
                + "To execute this operation, rerun the command with option --execute.");
            return 0;
        }

        return resourceService.resetPassword(namespace, user, output, commandSpec);
    }
}
