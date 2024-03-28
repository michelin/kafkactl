package com.michelin.kafkactl.hook;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.mixin.NamespaceMixin;
import com.michelin.kafkactl.mixin.VerboseMixin;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.LoginService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.ParameterException;

/**
 * Authenticated command.
 */
@Command
public abstract class AuthenticatedHook extends ValidCurrentContextHook {
    @Inject
    @ReflectiveAccess
    protected LoginService loginService;

    @Inject
    @ReflectiveAccess
    protected ApiResourcesService apiResourcesService;

    @Inject
    @ReflectiveAccess
    protected KafkactlConfig kafkactlConfig;

    @Mixin
    public NamespaceMixin namespaceMixinMixin;

    @Mixin
    public VerboseMixin verboseMixin;

    @Override
    public Integer onContextValid() throws IOException {
        if (!loginService.doAuthenticate(commandSpec, verboseMixin.verbose)) {
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
        return namespaceMixinMixin.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());
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
            throw new ParameterException(commandSpec.commandLine(),
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
