package com.michelin.kafkactl.command.auth;

import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Auth renew subcommand.
 */
@CommandLine.Command(name = "renew",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Renew the JWT token.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class AuthRenewSubcommand implements Callable<Integer> {
    @Inject
    public LoginService loginService;

    @CommandLine.ParentCommand
    public AuthSubcommand authSubcommand;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() throws IOException {
        loginService.deleteJwtFile();
        if (loginService.doAuthenticate(commandSpec, authSubcommand.kafkactlCommand.verbose)) {
            commandSpec.commandLine().getOut().println("JWT renewed successfully.");
            return 0;
        }

        commandSpec.commandLine().getErr().println("Failed to renew JWT.");
        return 1;
    }
}
