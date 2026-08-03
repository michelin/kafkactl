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

import static com.michelin.kafkactl.util.constant.ResourceKind.CONNECTOR_OFFSET_RESPONSE;

import com.michelin.kafkactl.model.Resource;
import java.util.stream.Stream;
import picocli.CommandLine.Command;

/** List connector offsets subcommand. */
@Command(
        name = "list-offsets",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "List connector offsets.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class ConnectorListOffsets extends ConnectorOffsetsCommand {

    /**
     * List offsets for a connector.
     *
     * @param namespace The namespace
     * @param connector The connector name
     * @return The connector offsets
     */
    @Override
    protected Stream<Resource> processConnector(String namespace, String connector) {
        return resourceService.listConnectorOffsets(namespace, connector, commandSpec).stream();
    }

    @Override
    protected String getResponseKind() {
        return CONNECTOR_OFFSET_RESPONSE;
    }
}
