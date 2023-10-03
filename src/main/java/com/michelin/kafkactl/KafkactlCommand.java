package com.michelin.kafkactl;

import static com.michelin.kafkactl.config.KafkactlConfig.KAFKACTL_CONFIG;

import com.michelin.kafkactl.services.SystemService;
import com.michelin.kafkactl.utils.VersionProvider;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.core.util.StringUtils;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Kafkactl command.
 */
@Command(name = "kafkactl",
    subcommands = {
        ApiResourcesSubcommand.class,
        ApplySubcommand.class,
        ConfigSubcommand.class,
        ConnectClustersSubcommand.class,
        ConnectorsSubcommand.class,
        DeleteRecordsSubcommand.class,
        DeleteSubcommand.class,
        DiffSubcommand.class,
        GetSubcommand.class,
        ImportSubcommand.class,
        ResetOffsetsSubcommand.class,
        SchemaSubcommand.class,
        UsersSubcommand.class
    },
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "These are common Kafkactl commands.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class KafkactlCommand implements Callable<Integer> {
    @Option(names = {"-v",
        "--verbose"}, description = "Enable the verbose mode.", scope = CommandLine.ScopeType.INHERIT)
    public boolean verbose;

    @Option(names = {"-n", "--namespace"}, description = "Override namespace defined in config or YAML resources.",
        scope = CommandLine.ScopeType.INHERIT)
    public Optional<String> optionalNamespace;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    /**
     * Main Micronaut method
     * There are 3 ways to configure kafkactl:
     * - Setup config file in $HOME/.kafkactl/config.yml
     * - Setup config file anywhere and set KAFKACTL_CONFIG=/path/to/config.yml
     * - No file but environment variables instead
     *
     * @param args Input arguments
     */
    public static void main(String[] args) {
        if (System.getenv().keySet().stream().noneMatch(s -> s.startsWith("KAFKACTL_"))) {
            SystemService.setProperty("micronaut.config.files",
                SystemService.getProperty("user.home") + "/.kafkactl/config.yml");
        }

        if (StringUtils.isNotEmpty(SystemService.getEnv(KAFKACTL_CONFIG))) {
            SystemService.setProperty("micronaut.config.files", SystemService.getEnv(KAFKACTL_CONFIG));
        }

        int exitCode = PicocliRunner.execute(KafkactlCommand.class, args);
        System.exit(exitCode);
    }

    /**
     * Run the "kafkactl" command.
     *
     * @return The command return code
     */
    public Integer call() {
        commandSpec.commandLine().getOut().println(new CommandLine(this).getUsageMessage());
        return 0;
    }
}
