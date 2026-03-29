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
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.ReflectiveAccess;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Resource. */
@Data
@Builder
@ReflectiveAccess
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    private String apiVersion;
    private String kind;
    private Metadata metadata;

    @JsonInclude(value = JsonInclude.Include.NON_ABSENT)
    private Map<String, Object> spec;

    private Object status;

    @Getter
    @Setter
    @Builder
    @ToString
    @ReflectiveAccess
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String name;
        private String namespace;
        private String cluster;
        private Map<String, String> labels;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Date creationTimestamp;

        private Status status;

        @Getter
        @Setter
        @Builder
        @ReflectiveAccess
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Status {
            private Phase phase;
            private String message;

            @JsonFormat(shape = JsonFormat.Shape.STRING)
            private Date lastUpdateTime;
        }

        public enum Phase {
            PENDING("Pending"),
            FAIL("Fail"),
            SUCCESS("Success");

            private final String name;

            Phase(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }

            @JsonCreator
            public static Phase fromString(String key) {
                for (Phase type : Phase.values()) {
                    if (type.name().equalsIgnoreCase(key)) {
                        return type;
                    }
                }
                return null;
            }
        }
    }
}
