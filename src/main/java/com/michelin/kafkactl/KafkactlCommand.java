package com.michelin.kafkactl;

import com.michelin.kafkactl.utils.VersionProvider;
import io.micronaut.configuration.picocli.PicocliRunner;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "kafkactl",
        subcommands =
                {
                        ApplySubcommand.class,
                        GetSubcommand.class,
                        DeleteSubcommand.class,
                        ApiResourcesSubcommand.class,
                        DiffSubcommand.class,
                        ResetOffsetsSubcommand.class,
                        DeleteRecordsSubcommand.class,
                        ImportSubcommand.class,
                        ConnectorsSubcommand.class,
                        SchemaSubcommand.class,
                        UsersSubcommand.class,
                        ConfigSubcommand.class,
                        ConnectClustersSubcommand.class
                },
        versionProvider = VersionProvider.class,
        mixinStandardHelpOptions = true)
public class KafkactlCommand implements Callable<Integer> {
    @Option(names = {"-v", "--verbose"}, description = "Enable the verbose mode.", scope = CommandLine.ScopeType.INHERIT)
    public boolean verbose;

    @Option(names = {"-n", "--namespace"}, description = "Override namespace defined in config or YAML resources.", scope = CommandLine.ScopeType.INHERIT)
    public Optional<String> optionalNamespace;

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    /**
     * Main Micronaut method
     * There are 3 ways to configure kafkactl:
     * - Setup config file in $HOME/.kafkactl/config.yml
     * - Setup config file anywhere and set KAFKACTL_CONFIG=/path/to/config.yml
     * - No file but environment variables instead
     * @param args Input arguments
     */
    public static void main(String[] args) {
        if (System.getenv().keySet().stream().noneMatch(s -> s.startsWith("KAFKACTL_"))) {
            System.setProperty("micronaut.config.files", System.getProperty("user.home") + "/.kafkactl/config.yml");
        }

        if (System.getenv("KAFKACTL_CONFIG") != null) {
            System.setProperty("micronaut.config.files", System.getenv("KAFKACTL_CONFIG"));
        }

        int exitCode = PicocliRunner.execute(KafkactlCommand.class, args);
        System.exit(exitCode);
    }

    /**
     * Run the "kafkactl" command
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    public Integer call() throws Exception {
        commandSpec.commandLine().getOut().println(new CommandLine(this).getUsageMessage());
        return 0;
    }
}
