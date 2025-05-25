/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.michelin.kafkactl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Output;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.model.Status;
import com.michelin.kafkactl.model.format.AgoFormat;
import com.michelin.kafkactl.model.format.DefaultFormat;
import com.michelin.kafkactl.model.format.OutputFormatStrategy;
import com.michelin.kafkactl.model.format.PeriodFormat;
import com.michelin.kafkactl.property.KafkactlProperties;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import picocli.CommandLine.Help;
import picocli.CommandLine.Model.CommandSpec;

/** Format service. */
@Singleton
public class FormatService {
    private final List<String> defaults =
            List.of("KIND:/kind", "NAME:/metadata/name", "AGE:/metadata/creationTimestamp%AGO");

    @Inject
    @ReflectiveAccess
    private KafkactlProperties kafkactlProperties;

    /**
     * Display a list of resources.
     *
     * @param kind The kind of resource
     * @param resources The list of resources
     * @param output The type of display
     * @param commandSpec The command spec used to print the output
     */
    public void displayList(String kind, List<Resource> resources, Output output, CommandSpec commandSpec) {
        if (output.equals(Output.TABLE)) {
            printTable(kind, resources, commandSpec);
        } else if (List.of(Output.YAML, Output.YML).contains(output)) {
            printYaml(resources, commandSpec);
        }
    }

    /**
     * Display a single resource.
     *
     * @param resource The resource
     * @param output The type of display
     * @param commandSpec The command spec used to print the output
     */
    public void displaySingle(Resource resource, Output output, CommandSpec commandSpec) {
        displayList(resource.getKind(), List.of(resource), output, commandSpec);
    }

    /**
     * Display an error for a given particular resource kind E.g., apply, delete, get
     *
     * @param exception The HTTP response error
     * @param kind The resource kind
     * @param name The resource name
     * @param commandSpec The command spec used to print the output
     */
    public void displayError(HttpClientResponseException exception, String kind, String name, CommandSpec commandSpec) {
        Optional<Status> statusOptional = exception.getResponse().getBody(Status.class);
        String prettyKind = prettifyKind(kind);
        String prettyName = prettifyName(name);

        if (statusOptional.isPresent()
                && statusOptional.get().getDetails() != null
                && !statusOptional.get().getDetails().getCauses().isEmpty()) {
            Status status = statusOptional.get();
            String causes = "\n - " + String.join("\n - ", status.getDetails().getCauses());
            commandSpec
                    .commandLine()
                    .getErr()
                    .printf(
                            "%s%s failed because %s (%s):%s%n",
                            prettyKind,
                            prettyName,
                            status.getMessage().toLowerCase(),
                            exception.getStatus().getCode(),
                            causes);
        } else {
            commandSpec
                    .commandLine()
                    .getErr()
                    .printf(
                            "%s%s failed because %s (%s).%n",
                            prettyKind,
                            prettyName,
                            exception.getMessage().toLowerCase(),
                            exception.getStatus().getCode());
        }
    }

    /**
     * Display a generic error.
     *
     * @param exception The HTTP client exception
     * @param commandSpec The command spec used to print the output
     */
    public void displayError(HttpClientResponseException exception, CommandSpec commandSpec) {
        Optional<Status> statusOptional = exception.getResponse().getBody(Status.class);
        if (statusOptional.isPresent()
                && statusOptional.get().getDetails() != null
                && !statusOptional.get().getDetails().getCauses().isEmpty()) {
            Status status = statusOptional.get();
            String causes = "\n - " + String.join("\n - ", status.getDetails().getCauses());
            commandSpec
                    .commandLine()
                    .getErr()
                    .printf(
                            "Failed because %s (%s):%s%n",
                            status.getMessage().toLowerCase(),
                            exception.getStatus().getCode(),
                            causes);
        } else {
            commandSpec
                    .commandLine()
                    .getErr()
                    .printf(
                            "Failed because %s (%s).%n",
                            exception.getMessage().toLowerCase(),
                            exception.getStatus().getCode());
        }
    }

    /**
     * Format a map to string.
     *
     * @param map The map to format
     * @return The map formatted string
     */
    public String formatMapToString(Map<String, String> map) {
        return map.toString().replaceAll("[{}\\s]", "");
    }

    /**
     * Format the display error message based on the search option and the resource name.
     *
     * @param apiResources The API resources list
     * @param search The search map
     * @param resourceName The resource name
     * @param commandSpec The command spec used to print the output
     */
    public void displayNoResource(
            List<ApiResource> apiResources, Map<String, String> search, String resourceName, CommandSpec commandSpec) {
        String resource = prettifyKind(apiResources.getFirst().getKind()).toLowerCase();

        if (search == null || search.isEmpty()) {
            if (resourceName.equals("*")) {
                commandSpec.commandLine().getOut().println("No " + resource + " to display.");
            } else {
                commandSpec
                        .commandLine()
                        .getOut()
                        .println("No " + resource + " matches name \"" + resourceName + "\".");
            }
        } else {
            if (resourceName.equals("*")) {
                commandSpec
                        .commandLine()
                        .getOut()
                        .println("No " + resource + " matches search \"" + formatMapToString(search) + "\".");
            } else {
                commandSpec
                        .commandLine()
                        .getOut()
                        .println("No " + resource + " matches name \"" + resourceName + "\" and search \""
                                + formatMapToString(search) + "\".");
            }
        }
    }

    /**
     * Print the list of resources to table format.
     *
     * @param kind The kind of resources
     * @param resources The list of resources
     * @param commandSpec The command spec used to print the output
     */
    private void printTable(String kind, List<Resource> resources, CommandSpec commandSpec) {
        String hyphenatedKind = StringConvention.HYPHENATED.format(kind);
        List<String> formats = kafkactlProperties.getTableFormat().getOrDefault(hyphenatedKind, defaults);

        PrettyTextTable ptt = new PrettyTextTable(formats, resources);
        commandSpec.commandLine().getOut().println(ptt);
    }

    /**
     * Print the list of resources to yaml format.
     *
     * @param resources The list of resources
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
        return kind.substring(0, 1).toUpperCase()
                + kind.substring(1).replaceAll("(.)([A-Z])", "$1 $2").toLowerCase();
    }

    /**
     * Prettify name.
     *
     * @param name The name
     * @return The prettified name
     */
    public String prettifyName(String name) {
        return name.equals("*") ? "" : " \"" + name + "\"";
    }

    /** Pretty text table. */
    public static class PrettyTextTable {
        private final List<PrettyTextTableColumn> columns = new ArrayList<>();
        private final List<String[]> rows = new ArrayList<>();

        /**
         * Constructor.
         *
         * @param formats The list of formats
         * @param resources The list of resources
         */
        public PrettyTextTable(List<String> formats, List<Resource> resources) {
            // 1. Prepare header columns
            formats.forEach(item -> {
                String[] elements = item.split(":");
                if (elements.length != 2) {
                    throw new IllegalStateException("Expected line with format 'NAME:JSONPOINTER[%TRANSFORM]', but got "
                            + Arrays.toString(elements) + " instead.");
                }
                columns.add(new PrettyTextTableColumn(columns.isEmpty() ? 0 : 2, elements));
            });

            // 2. Prepare rows and update column sizes
            ObjectMapper mapper = new ObjectMapper();
            resources.forEach(resource -> {
                JsonNode node = mapper.valueToTree(resource);
                rows.add(columns.stream().map(column -> column.transform(node)).toArray(String[]::new));
            });
        }

        @Override
        public String toString() {
            Help.Column[] sizedColumns = this.columns.stream()
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

                String[] field = elements[1].split("%");
                if (field.length > 1) {
                    switch (field[1]) {
                        case "AGO":
                            this.outputFormat = new AgoFormat(field[0]);
                            break;
                        case "PERIOD":
                            this.outputFormat = new PeriodFormat(field[0]);
                            break;
                        default:
                            break;
                    }
                } else {
                    this.outputFormat = new DefaultFormat(field[0]);
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
