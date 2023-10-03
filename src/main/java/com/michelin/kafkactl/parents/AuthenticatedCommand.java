package com.michelin.kafkactl.parents;

import com.michelin.kafkactl.KafkactlCommand;
import com.michelin.kafkactl.services.LoginService;
import jakarta.inject.Inject;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Authenticated command.
 */
@CommandLine.Command
public abstract class AuthenticatedCommand implements Callable<Integer> {
    @Inject
    protected LoginService loginService;

    @CommandLine.ParentCommand
    protected KafkactlCommand kafkactlCommand;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() throws Exception {
        if (!loginService.doAuthenticate(commandSpec, kafkactlCommand.verbose)) {
            return 1;
        }

        return onAuthSuccess();
    }

    public abstract Integer onAuthSuccess() throws Exception;
}
