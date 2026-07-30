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
package com.michelin.kafkactl.command.connector;

import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECTOR_RESET_OFFSETS_RESPONSE;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.model.request.ConnectorOffsets;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Alter connector offsets subcommand. */
@Command(
        name = "alter-offsets",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Alter or partially reset connector offsets.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class ConnectorAlterOffsets extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Parameters(index = "0", description = "Connector name.")
    public String connector;

    @Option(
            names = {"-f", "--file"},
            description = "YAML file containing the connector offsets payload.",
            required = true)
    public File file;

    @Override
    public Integer onAuthSuccess() throws IOException {
        ConnectorOffsets offsets = new Yaml(new Constructor(ConnectorOffsets.class, new LoaderOptions()))
                .load(Files.readString(file.toPath()));

        return resourceService
                .alterConnectorOffsets(getNamespace(), connector, offsets, commandSpec)
                .map(this::displayResponse)
                .orElse(1);
    }

    private int displayResponse(Resource response) {
        formatService.displayList(CONNECTOR_RESET_OFFSETS_RESPONSE, List.of(response), TABLE, commandSpec);
        return 0;
    }
}
