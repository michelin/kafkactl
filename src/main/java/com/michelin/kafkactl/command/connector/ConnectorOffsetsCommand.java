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

import com.michelin.kafkactl.model.Resource;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import java.util.List;
import java.util.stream.Stream;
import picocli.CommandLine.Parameters;

/** Base command for connector offset operations. */
public abstract class ConnectorOffsetsCommand extends ConnectorCommand {
    @Parameters(
            index = "0..*",
            description = "Connector names separated by space or \"all\" for all connectors.",
            arity = "1..*")
    public List<String> connectors;

    @Override
    public Integer onAuthSuccess() {
        String namespace = getNamespace();

        try {
            connectors = resolveConnectors(connectors, namespace);
            List<Resource> responses = connectors.stream()
                    .flatMap(connector -> processConnector(namespace, connector))
                    .toList();

            if (!responses.isEmpty()) {
                formatService.displayList(getResponseKind(), responses, TABLE, commandSpec);
                return 0;
            }

            return 1;
        } catch (HttpClientResponseException exception) {
            formatService.displayError(exception, commandSpec);
            return 1;
        }
    }

    /**
     * Process offsets for a connector.
     *
     * @param namespace The namespace
     * @param connector The connector name
     * @return The operation responses
     */
    protected abstract Stream<Resource> processConnector(String namespace, String connector);

    /**
     * Get the operation response kind.
     *
     * @return The response kind
     */
    protected abstract String getResponseKind();
}
