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
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.text.ParseException;
import java.util.Date;
import lombok.AllArgsConstructor;
import org.ocpsoft.prettytime.PrettyTime;

/** Ago format. */
@AllArgsConstructor
public class AgoFormat implements OutputFormatStrategy {
    private String jsonPointer;

    @Override
    public String display(JsonNode node) {
        String output;
        JsonNode cell = node.at(this.jsonPointer);

        try {
            StdDateFormat sdf = new StdDateFormat();
            Date d = sdf.parse(cell.asText());
            output = new PrettyTime().format(d);
        } catch (ParseException e) {
            output = EMPTY_STRING;
        }

        return output;
    }
}
