package com.michelin.kafkactl;

import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.LoginService;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "api-resources", description = "Print the supported API resources on the server")
public class ApiResourcesSubcommand implements Callable<Integer> {
    @Inject
    public ApiResourcesService apiResourcesService;

    @Inject
    public LoginService loginService;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "api-resources" command
     * @return The command return code
     */
    @Override
    public Integer call() {
        boolean authenticated = loginService.doAuthenticate();
        if (!authenticated) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Login failed");
        }

        CommandLine.Help.TextTable textTable = CommandLine.Help.TextTable.forColumns(
                CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.AUTO),
                new CommandLine.Help.Column(30, 2, CommandLine.Help.Column.Overflow.SPAN),
                new CommandLine.Help.Column(30, 2, CommandLine.Help.Column.Overflow.SPAN),
                new CommandLine.Help.Column(30, 2, CommandLine.Help.Column.Overflow.SPAN));

        textTable.addRowValues("KIND", "NAMES", "NAMESPACED");

        apiResourcesService.getListResourceDefinition().forEach(rd ->
                textTable.addRowValues(rd.getKind(), String.join(",", rd.getNames()), String.valueOf(rd.isNamespaced())));

        System.out.println(textTable);
        return 0;
    }
}
