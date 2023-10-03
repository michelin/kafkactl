package com.michelin.kafkactl.parents;

import com.michelin.kafkactl.KafkactlCommand;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.LoginService;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine;

/**
 * Authenticated command.
 */
@CommandLine.Command
public abstract class AuthenticatedCommand implements Callable<Integer> {
    @Inject
    protected LoginService loginService;

    @Inject
    public ApiResourcesService apiResourcesService;

    @Inject
    public KafkactlConfig kafkactlConfig;

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

    protected String getNamespace() {
        return kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());
    }

    protected void validateNamespace(List<Resource> resources) {
        List<Resource> namespaceMismatch = resources
            .stream()
            .filter(resource -> resource.getMetadata().getNamespace() != null
                && !resource.getMetadata().getNamespace().equals(getNamespace()))
            .toList();

        if (!namespaceMismatch.isEmpty()) {
            String invalid = namespaceMismatch
                .stream()
                .map(resource -> resource.getKind() + "/" + resource.getMetadata().getName())
                .distinct()
                .collect(Collectors.joining(", "));
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                "Namespace mismatch between Kafkactl and YAML document " + invalid + ".");
        }
    }

    public abstract Integer onAuthSuccess() throws Exception;
}
