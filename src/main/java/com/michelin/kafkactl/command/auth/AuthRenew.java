package com.michelin.kafkactl.command.auth;

import com.michelin.kafkactl.hook.HelpHook;
import com.michelin.kafkactl.mixin.VerboseMixin;
import com.michelin.kafkactl.service.LoginService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Auth renew subcommand.
 */
@Command(name = "renew",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Renew the JWT token.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true)
public class AuthRenew extends HelpHook implements Callable<Integer> {
    @Inject
    @ReflectiveAccess
    private LoginService loginService;

    @Mixin
    public VerboseMixin verboseMixin;

    @Spec
    public CommandSpec commandSpec;

    @Override
    public Integer call() throws IOException {
        loginService.deleteJwtFile();
        if (loginService.doAuthenticate(commandSpec, verboseMixin.verbose)) {
            commandSpec.commandLine().getOut().println("JWT renewed successfully.");
            return 0;
        }

        commandSpec.commandLine().getErr().println("Failed to renew JWT.");
        return 1;
    }
}
