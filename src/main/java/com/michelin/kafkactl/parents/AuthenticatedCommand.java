package com.michelin.kafkactl.parents;

import com.michelin.kafkactl.ConfigSubcommand;
import com.michelin.kafkactl.KafkactlCommand;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.ConfigService;
import com.michelin.kafkactl.services.LoginService;
import jakarta.inject.Inject;
import java.io.IOException;
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
    public LoginService loginService;

    @Inject
    public ApiResourcesService apiResourcesService;

    @Inject
    public ConfigService configService;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the command.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    @Override
    public Integer call() throws IOException {
        if (!configService.isCurrentContextValid()) {
            CommandLine configSubcommand = new CommandLine(new ConfigSubcommand());
            commandSpec.commandLine().getErr().println("No valid current context found. Use \"kafkactl "
                + configSubcommand.getCommandName() + " "
                + ConfigSubcommand.ConfigAction.USE_CONTEXT + "\" to set a valid context.");
            return 1;
        }

        if (!loginService.doAuthenticate(commandSpec, kafkactlCommand.verbose)) {
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
        return kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());
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
