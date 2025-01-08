package com.michelin.kafkactl.command.auth;

import static com.michelin.kafkactl.util.constant.ResourceKind.AUTH_INFO;

import com.michelin.kafkactl.hook.HelpHook;
import com.michelin.kafkactl.model.JwtContent;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.LoginService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Auth info subcommand.
 */
@Command(name = "info",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@: ",
    description = "Get the JWT token information.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true)
public class AuthInfo extends HelpHook implements Callable<Integer> {
    @Inject
    @ReflectiveAccess
    private LoginService loginService;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Spec
    public CommandSpec commandSpec;

    @Option(names = {"-o",
        "--output"}, description = "Output format. One of: yaml|table", defaultValue = "table")
    public String output;

    @Override
    public Integer call() throws IOException {
        if (!loginService.jwtFileExists()) {
            commandSpec.commandLine().getOut().println("No JWT found. You are not authenticated.");
        } else {
            JwtContent jwtContent = loginService.readJwtFile();

            boolean isAdmin = (!jwtContent.getRoles().isEmpty() && jwtContent.getRoles().contains("isAdmin()"));
            commandSpec.commandLine().getOut().println((isAdmin ? "Admin " : "User ")
                + jwtContent.getSub() + " authenticated.");

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(jwtContent.getExp() * 1000);
            commandSpec.commandLine().getOut().println("Session valid until " + calendar.getTime() + ".");

            if (!jwtContent.getRoleBindings().isEmpty()) {
                List<Resource> roleBindings = jwtContent.getRoleBindings()
                    .stream()
                    .flatMap(roleBinding -> roleBinding.getNamespaces()
                        .stream()
                        .map(namespace -> Resource.builder()
                            .spec(Map.of(
                                "namespace", namespace,
                                "verbs", roleBinding.getVerbs(),
                                "resources", roleBinding.getResourceTypes()
                            ))
                            .build()
                        )
                    )
                    .sorted(Comparator.comparing(roleBinding -> (String) roleBinding.getSpec().get("namespace"),
                        Comparator.naturalOrder()))
                    .toList();

                formatService.displayList(AUTH_INFO, roleBindings, output, commandSpec);
            }
        }

        return 0;
    }
}
