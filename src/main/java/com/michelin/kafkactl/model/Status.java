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
package com.michelin.kafkactl.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.micronaut.core.annotation.ReflectiveAccess;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Status. */
@Data
@Builder
@ReflectiveAccess
@NoArgsConstructor
@AllArgsConstructor
public class Status {
    @Builder.Default
    private String apiVersion = "v1";

    @Builder.Default
    private String kind = "Status";

    private StatusPhase status;
    private String message;
    private String reason;
    private StatusDetails details;
    private int code;

    /** Status phase. */
    public enum StatusPhase {
        SUCCESS,
        FAILED;

        /**
         * Build status phase from string. This is because Ns4Kafka returns capitalised status phases.
         *
         * @param key the key
         * @return the status phase
         */
        @JsonCreator
        public static StatusPhase fromString(String key) {
            for (StatusPhase type : StatusPhase.values()) {
                if (type.name().equalsIgnoreCase(key)) {
                    return type;
                }
            }
            return null;
        }
    }

    /** Status details. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDetails {
        private String name;
        private String kind;
        private List<String> causes;
    }
}
