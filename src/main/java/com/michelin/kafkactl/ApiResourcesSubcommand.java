package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.ApiResourcesService;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.utils.VersionProvider;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.michelin.kafkactl.services.FormatService.TABLE;
import static com.michelin.kafkactl.utils.constants.ConstantKind.RESOURCE_DEFINITION;

@CommandLine.Command(name = "api-resources",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@:%n%n",
        description = "Print the supported API resources on the server.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true,
        versionProvider = VersionProvider.class,
        mixinStandardHelpOptions = true)
public class ApiResourcesSubcommand implements Callable<Integer> {
    @Inject
    public ApiResourcesService apiResourcesService;

    @Inject
    public LoginService loginService;

    @Inject
    public FormatService formatService;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "api-resources" command
     * @return The command return code
     */
    @Override
    public Integer call() {
        if (!loginService.doAuthenticate(commandSpec, kafkactlCommand.verbose)) {
            return 1;
        }

        try {
            List<Resource> resources = apiResourcesService.listResourceDefinitions()
                    .stream()
                    .map(apiResource -> Resource.builder()
                            .metadata(ObjectMeta.builder()
                                    .name(apiResource.getKind())
                                    .build())
                            .spec(Map.of("names", String.join(",", apiResource.getNames()),
                                    "namespaced", String.valueOf(apiResource.isNamespaced()),
                                    "synchronizable", String.valueOf(apiResource.isSynchronizable())))
                            .build())
                    .collect(Collectors.toList());

            formatService.displayList(RESOURCE_DEFINITION, resources, TABLE, commandSpec);
            return 0;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }
}
