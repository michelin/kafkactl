package com.michelin.kafkactl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.model.Status;
import com.michelin.kafkactl.model.format.AgoFormat;
import com.michelin.kafkactl.model.format.DefaultFormat;
import com.michelin.kafkactl.model.format.OutputFormatStrategy;
import com.michelin.kafkactl.model.format.PeriodFormat;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import picocli.CommandLine.Help;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Format service.
 */
@Singleton
public class FormatService {
    public static final String YAML = "yaml";
    public static final String TABLE = "table";
    private final List<String> defaults =
        List.of("KIND:/kind", "NAME:/metadata/name", "AGE:/metadata/creationTimestamp%AGO");

    @Inject
    @ReflectiveAccess
    private KafkactlConfig kafkactlConfig;

    /**
     * Display a list of resources.
     *
     * @param kind        The kind of resource
     * @param resources   The list of resources
     * @param output      The type of display
     * @param commandSpec The command spec used to print the output
     */
    public void displayList(String kind, List<Resource> resources, String output, CommandSpec commandSpec) {
        if (output.equals(TABLE)) {
            printTable(kind, resources, commandSpec);
        } else if (output.equals(YAML)) {
            printYaml(resources, commandSpec);
        }
    }

    /**
     * Display a single resource.
     *
     * @param resource    The resource
     * @param output      The type of display
     * @param commandSpec The command spec used to print the output
     */
    public void displaySingle(Resource resource, String output, CommandSpec commandSpec) {
        displayList(resource.getKind(), List.of(resource), output, commandSpec);
    }

    /**
     * Display an error for a given particular resource kind
     * E.g., apply, delete, get
     *
     * @param exception   The HTTP response error
     * @param kind        The resource kind
     * @param name        The resource name
     * @param commandSpec The command spec used to print the output
     */
    public void displayError(HttpClientResponseException exception, String kind, String name, CommandSpec commandSpec) {
        Optional<Status> statusOptional = exception.getResponse().getBody(Status.class);
        String prettyKind = prettifyKind(kind);
        if (statusOptional.isPresent() && statusOptional.get().getDetails() != null
            && !statusOptional.get().getDetails().getCauses().isEmpty()) {
            Status status = statusOptional.get();
            String causes = "\n - " + String.join("\n - ", status.getDetails().getCauses());
            commandSpec.commandLine().getErr()
                .printf("%s \"%s\" failed because %s (%s):%s%n", prettyKind, name, status.getMessage().toLowerCase(),
                    exception.getStatus().getCode(), causes);
        } else {
            commandSpec.commandLine().getErr()
                .printf("%s \"%s\" failed because %s (%s).%n", prettyKind, name, exception.getMessage().toLowerCase(),
                    exception.getStatus().getCode());
        }
    }

    /**
     * Display an error for a given kind of resources.
     *
     * @param exception   The HTTP response error
     * @param kind        The resource kind
     * @param commandSpec The command spec used to print the output
     */
    public void displayError(HttpClientResponseException exception, String kind, CommandSpec commandSpec) {
        Optional<Status> statusOptional = exception.getResponse().getBody(Status.class);
        String prettyKind = prettifyKind(kind);
        if (statusOptional.isPresent() && statusOptional.get().getDetails() != null
            && !statusOptional.get().getDetails().getCauses().isEmpty()) {
            Status status = statusOptional.get();
            String causes = "\n - " + String.join("\n - ", status.getDetails().getCauses());
            commandSpec.commandLine().getErr().printf("%s failed because %s (%s):%s%n", prettyKind,
                status.getMessage().toLowerCase(), exception.getStatus().getCode(), causes);
        } else {
            commandSpec.commandLine().getErr().printf("%s failed because %s (%s).%n", prettyKind,
                exception.getMessage().toLowerCase(), exception.getStatus().getCode());
        }
    }

    /**
     * Display a generic error.
     *
     * @param exception   The HTTP client exception
     * @param commandSpec The command spec used to print the output
     */
    public void displayError(HttpClientResponseException exception, CommandSpec commandSpec) {
        Optional<Status> statusOptional = exception.getResponse().getBody(Status.class);
        if (statusOptional.isPresent() && statusOptional.get().getDetails() != null
            && !statusOptional.get().getDetails().getCauses().isEmpty()) {
            Status status = statusOptional.get();
            String causes = "\n - " + String.join("\n - ", status.getDetails().getCauses());
            commandSpec.commandLine().getErr().printf("Failed because %s (%s):%s%n", status.getMessage().toLowerCase(),
                exception.getStatus().getCode(),
                causes);
        } else {
            commandSpec.commandLine().getErr().printf("Failed because %s (%s).%n", exception.getMessage().toLowerCase(),
                exception.getStatus().getCode());
        }
    }

    /**
     * Print the list of resources to table format.
     *
     * @param kind        The kind of resources
     * @param resources   The list of resources
     * @param commandSpec The command spec used to print the output
     */
    private void printTable(String kind, List<Resource> resources, CommandSpec commandSpec) {
        String hyphenatedKind = StringConvention.HYPHENATED.format(kind);
        List<String> formats = kafkactlConfig.getTableFormat().getOrDefault(hyphenatedKind, defaults);

        PrettyTextTable ptt = new PrettyTextTable(formats, resources);
        commandSpec.commandLine().getOut().println(ptt);
    }

    /**
     * Print the list of resources to yaml format.
     *
     * @param resources   The list of resources
     * @param commandSpec The command spec used to print the output
     */
    private void printYaml(List<Resource> resources, CommandSpec commandSpec) {
        DumperOptions options = new DumperOptions();
        options.setExplicitStart(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer(new DumperOptions());
        representer.addClassTag(Resource.class, Tag.MAP);
        Yaml yaml = new Yaml(representer, options);
        commandSpec.commandLine().getOut().println(yaml.dumpAll(resources.iterator()));
    }

    /**
     * Prettify kind.
     *
     * @param kind The kind
     * @return The prettified kind
     */
    public String prettifyKind(String kind) {
        return kind.substring(0, 1).toUpperCase() + kind.substring(1).replaceAll("(.)([A-Z])", "$1 $2").toLowerCase();
    }

    /**
     * Pretty text table.
     */
    public static class PrettyTextTable {
        private final List<PrettyTextTableColumn> columns = new ArrayList<>();
        private final List<String[]> rows = new ArrayList<>();

        /**
         * Constructor.
         *
         * @param formats   The list of formats
         * @param resources The list of resources
         */
        public PrettyTextTable(List<String> formats, List<Resource> resources) {
            // 1. Prepare header columns
            formats.forEach(item -> {
                String[] elements = item.split(":");
                if (elements.length != 2) {
                    throw new IllegalStateException(
                        "Expected line with format 'NAME:JSONPOINTER[%TRANSFORM]', but got "
                            + Arrays.toString(elements) + " instead.");
                }
                columns.add(new PrettyTextTableColumn(columns.isEmpty() ? 0 : 2, elements));
            });

            // 2. Prepare rows and update column sizes
            ObjectMapper mapper = new ObjectMapper();
            resources.forEach(resource -> {
                JsonNode node = mapper.valueToTree(resource);
                rows.add(columns.stream()
                    .map(column -> column.transform(node))
                    .toArray(String[]::new)
                );
            });
        }

        @Override
        public String toString() {
            Help.Column[] sizedColumns = this.columns
                .stream()
                .map(column -> new Help.Column(column.size, column.indent, Help.Column.Overflow.SPAN))
                .toArray(Help.Column[]::new);

            Help.TextTable tt = Help.TextTable.forColumns(Help.defaultColorScheme(Help.Ansi.AUTO), sizedColumns);

            // Create Header Row
            tt.addRowValues(this.columns.stream().map(column -> column.header).toArray(String[]::new));
            // Create Data Rows
            this.rows.forEach(tt::addRowValues);

            return tt.toString();
        }

        static class PrettyTextTableColumn {
            private final String header;
            private final int indent;
            private int size = -1;
            private OutputFormatStrategy outputFormat;

            public PrettyTextTableColumn(int indent, String... elements) {
                this.header = elements[0];
                this.indent = indent;

                if (elements[1].contains("%")) {
                    String jsonPointer = elements[1].split("%")[0];
                    String format = elements[1].split("%")[1];
                    if (format.equals("AGO")) {
                        this.outputFormat = new AgoFormat(jsonPointer);
                    } else if (format.equals("PERIOD")) {
                        this.outputFormat = new PeriodFormat(jsonPointer);
                    }
                } else {
                    this.outputFormat = new DefaultFormat(elements[1]);
                }

                // Size should consider headers
                this.size = Math.max(this.size, this.header.length() + indent);
            }

            public String transform(JsonNode node) {
                String output = this.outputFormat.display(node);
                // Check size for later
                size = Math.max(size, output.length() + indent);
                return output;
            }
        }
    }
}
