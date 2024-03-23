package com.michelin.kafkactl.hook;

import com.michelin.kafkactl.Kafkactl;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.LoginService;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import picocli.CommandLine;

/**
 * Authenticated command.
 */
@CommandLine.Command
public abstract class AuthenticatedHook extends ValidCurrentContextHook {
    @Inject
    public LoginService loginService;

    @Inject
    public ApiResourcesService apiResourcesService;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer onContextValid() throws IOException {
        if (!loginService.doAuthenticate(commandSpec, Kafkactl.verbose)) {
            return 1;
        }

        return onAuthSuccess();
    }

    /**
     * Gets the current namespace.
     *
     * @return The current namespace
     */
    protected String getNamespace() {
        return Kafkactl.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());
    }

    /**
     * Validate the namespace does not mismatch between Kafkactl and YAML document.
     *
     * @param resources The resources
     */
    protected void validateNamespace(List<Resource> resources) {
        List<Resource> namespaceMismatch = resources
            .stream()
            .filter(resource -> resource.getMetadata().getNamespace() != null
                && !resource.getMetadata().getNamespace().equals(getNamespace()))
            .toList();

        if (!namespaceMismatch.isEmpty()) {
            String invalid = namespaceMismatch
                .stream()
                .map(resource -> "\"" + resource.getKind() + "/" + resource.getMetadata().getName() + "\"")
                .distinct()
                .collect(Collectors.joining(", "));
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                "Namespace mismatch between Kafkactl configuration and YAML resource(s): " + invalid + ".");
        }
    }

    /**
     * Run after authentication success.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    public abstract Integer onAuthSuccess() throws IOException;
}
