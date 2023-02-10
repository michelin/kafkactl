package com.michelin.kafkactl.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.michelin.kafkactl.KafkactlConfig;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.models.Status;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.ocpsoft.prettytime.PrettyTime;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import picocli.CommandLine;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Singleton
public class FormatService {
    public static final String YAML = "yaml";
    public static final String TABLE = "table";
    private final List<String> defaults = List.of("KIND:/kind", "NAME:/metadata/name", "AGE:/metadata/creationTimestamp%AGO");

    @Inject
    public KafkactlConfig kafkactlConfig;

    /**
     * Display a list of resources
     * @param kind The kind of resource
     * @param resources The list of resources
     * @param output The type of display
     */
    public void displayList(String kind, List<Resource> resources, String output, CommandLine.Model.CommandSpec commandSpec) {
        if (output.equals(TABLE)) {
            printTable(kind, resources, commandSpec);
        } else if (output.equals(YAML)) {
            printYaml(resources, commandSpec);
        }
    }

    /**
     * Display a single resource
     * @param resource The resource
     * @param output The type of display
     */
    public void displaySingle(Resource resource, String output, CommandLine.Model.CommandSpec commandSpec) {
        displayList(resource.getKind(), List.of(resource), output, commandSpec);
    }

    /**
     * Display an error
     * @param e The HTTP response error
     * @param kind The resource kind
     * @param name The resource name
     */
    public void displayError(HttpClientResponseException e, String kind, String name, CommandLine.Model.CommandSpec commandSpec) {
        Optional<Status> statusOptional = e.getResponse().getBody(Status.class);
        if (statusOptional.isPresent() && statusOptional.get().getDetails() != null && !statusOptional.get().getDetails().getCauses().isEmpty()) {
            Status status = statusOptional.get();
            String causes = "\n - " + String.join("\n - ", status.getDetails().getCauses());

            commandSpec.commandLine().getErr().printf("Failed %s/%s %s for cause(s): %s%n", kind, name, status.getMessage(), causes);
        } else {
            displayError(e.getMessage(), kind, name, commandSpec);
        }
    }

    /**
     * Display an error
     * @param errorMessage The error message
     * @param kind The resource kind
     * @param name The resource name
     */
    public void displayError(String errorMessage, String kind, String name, CommandLine.Model.CommandSpec commandSpec) {
        commandSpec.commandLine().getErr().printf("Failed %s/%s %s.%n", kind, name, errorMessage);
    }

    /**
     * Print the list of resources to table format
     * @param kind The kind of resources
     * @param resources The list of resources
     */
    private void printTable(String kind, List<Resource> resources, CommandLine.Model.CommandSpec commandSpec) {
        String hyphenatedKind = StringConvention.HYPHENATED.format(kind);
        List<String> formats = kafkactlConfig.getTableFormat().getOrDefault(hyphenatedKind, defaults);

        PrettyTextTable ptt = new PrettyTextTable(formats, resources);
        commandSpec.commandLine().getOut().println(ptt);
    }

    /**
     * Print the list of resources to yaml format
     * @param resources The list of resources
     */
    private void printYaml(List<Resource> resources, CommandLine.Model.CommandSpec commandSpec) {
        DumperOptions options = new DumperOptions();
        options.setExplicitStart(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer(new DumperOptions());
        representer.addClassTag(Resource.class, Tag.MAP);
        Yaml yaml = new Yaml(representer, options);
        commandSpec.commandLine().getOut().println(yaml.dumpAll(resources.iterator()));
    }

    public static class PrettyTextTable {
        private final List<PrettyTextTableColumn> columns = new ArrayList<>();
        private final List<String[]> rows = new ArrayList<>();

        public PrettyTextTable(List<String> formats, List<Resource> resources) {
            // 1. Prepare header columns
            formats.forEach(item -> {
                String[] elements = item.split(":");
                if (elements.length != 2) {
                    throw new IllegalStateException("Expected line with format 'NAME:JSONPOINTER[%TRANSFORM]', but got " + Arrays.toString(elements) + " instead.");
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
            CommandLine.Help.Column[] sizedColumns = this.columns
                    .stream()
                    .map(column -> new CommandLine.Help.Column(column.size, column.indent, CommandLine.Help.Column.Overflow.SPAN))
                    .toArray(CommandLine.Help.Column[]::new);

            CommandLine.Help.TextTable tt = CommandLine.Help.TextTable.forColumns(
                    CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.AUTO),
                    sizedColumns);

            // Create Header Row
            tt.addRowValues(this.columns.stream().map(column -> column.header).toArray(String[]::new));
            // Create Data Rows
            this.rows.forEach(tt::addRowValues);

            return tt.toString();
        }

        static class PrettyTextTableColumn {
            private final String header;
            private final String jsonPointer;
            private final String transform;
            private int size = -1;
            private final int indent;

            public PrettyTextTableColumn(int indent, String... elements) {
                this.header = elements[0];
                this.indent = indent;

                if (elements[1].contains("%")) {
                    this.jsonPointer = elements[1].split("%")[0];
                    this.transform = elements[1].split("%")[1];
                } else {
                    this.jsonPointer = elements[1];
                    this.transform = "NONE";
                }
                // Size should consider headers
                this.size = Math.max(this.size, this.header.length() + indent);
            }

            public String transform(JsonNode node) {
                String output;
                JsonNode cell = node.at(this.jsonPointer);
                switch (this.transform) {
                    case "AGO":
                        try {
                            StdDateFormat sdf = new StdDateFormat();
                            Date d = sdf.parse(cell.asText());
                            output = new PrettyTime().format(d);
                        } catch ( ParseException e) {
                            output = cell.asText();
                        }
                        break;
                    case "PERIOD":
                        try {
                            long ms = Long.parseLong(cell.asText());
                            long days = TimeUnit.MILLISECONDS.toDays(ms);
                            long hours = TimeUnit.MILLISECONDS.toHours(ms - TimeUnit.DAYS.toMillis(days));
                            long minutes = TimeUnit.MILLISECONDS.toMinutes(ms - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours));
                            output = days > 0 ? (days + "d") : "";
                            output += hours > 0 ? (hours + "h") : "";
                            output += minutes > 0 ? (minutes + "m") : "";
                        } catch (NumberFormatException e) {
                            output = "err:" + cell;
                        }
                        break;
                    case "NONE":
                    default:
                        if (cell.isArray()) {
                            List<String> childs = new ArrayList<>();
                            cell.elements().forEachRemaining(jsonNode -> childs.add(jsonNode.asText()));
                            output = "[" + String.join(",", childs) + "]";
                        } else {
                            output = cell.asText(StringUtils.EMPTY_STRING);
                        }
                        break;
                }
                // Check size for later
                size = Math.max(size, output.length() + indent);
                return output;
            }
        }
    }
}
