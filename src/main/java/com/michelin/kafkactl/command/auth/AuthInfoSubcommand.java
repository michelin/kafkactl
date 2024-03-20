package com.michelin.kafkactl.command.auth;

import static com.michelin.kafkactl.utils.constants.ConstantKind.AUTH_INFO;

import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Auth info subcommand.
 */
@CommandLine.Command(name = "info",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Get the JWT token information.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class AuthInfoSubcommand implements Callable<Integer> {
    @Inject
    public LoginService loginService;

    @Inject
    public FormatService formatService;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Option(names = {"-o",
        "--output"}, description = "Output format. One of: yaml|table", defaultValue = "table")
    public String output;

    @Override
    public Integer call() throws IOException {
        if (!loginService.jwtFileExists()) {
            commandSpec.commandLine().getOut().println("No JWT found. You are not authenticated.");
        } else {
            formatService.displayList(AUTH_INFO, loginService.readJwtFile(), output, commandSpec);
        }

        return 0;
    }
}
