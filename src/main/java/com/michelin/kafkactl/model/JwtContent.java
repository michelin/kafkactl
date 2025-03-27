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

import io.micronaut.core.annotation.ReflectiveAccess;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Jwt content. */
@Data
@Builder
@ReflectiveAccess
@NoArgsConstructor
@AllArgsConstructor
public class JwtContent {
    private String sub;
    private Long exp;

    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Builder.Default
    private List<RoleBinding> roleBindings = new ArrayList<>();

    /** Role binding. */
    @Data
    @Builder
    @ReflectiveAccess
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleBinding {
        private List<String> namespaces;
        private List<Verb> verbs;
        private List<String> resourceTypes;

        /** Verb. */
        public enum Verb {
            GET,
            POST,
            PUT,
            DELETE
        }
    }
}
