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
package com.michelin.kafkactl.model.format;

import static io.micronaut.core.util.StringUtils.EMPTY_STRING;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

/** Default format. */
@AllArgsConstructor
public class DefaultFormat implements OutputFormatStrategy {
    private String jsonPointer;

    @Override
    public String display(JsonNode node) {
        String output;
        JsonNode cell = node.at(this.jsonPointer);

        if (cell.isArray()) {
            List<String> children = new ArrayList<>();
            cell.elements().forEachRemaining(jsonNode -> children.add(jsonNode.asText()));
            output = String.join(",", children);
        } else {
            output = cell.getNodeType().equals(JsonNodeType.NULL) ? EMPTY_STRING : cell.asText();
        }

        return output;
    }
}
