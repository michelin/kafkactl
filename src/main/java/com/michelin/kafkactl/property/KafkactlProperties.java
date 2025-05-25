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
package com.michelin.kafkactl.property;

import com.michelin.kafkactl.service.SystemService;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.util.StringUtils;
import java.io.File;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Kafkactl properties. */
@Getter
@Setter
@ConfigurationProperties("kafkactl")
public class KafkactlProperties {
    public static final String KAFKACTL_CONFIG = "KAFKACTL_CONFIG";

    private String api;
    private String userToken;
    private String currentNamespace;
    private List<ContextsProperties> contexts;
    private String version;

    @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
    private Map<String, List<String>> tableFormat;

    /**
     * Get the current user config directory.
     *
     * @return The config directory
     */
    public String getConfigDirectory() {
        if (StringUtils.isNotEmpty(SystemService.getEnv(KAFKACTL_CONFIG))) {
            String parent = new File(SystemService.getEnv(KAFKACTL_CONFIG)).getParent();
            return parent != null ? parent : ".";
        }

        return SystemService.getProperty("user.home") + "/.kafkactl";
    }

    /**
     * Get the current user config full path.
     *
     * @return The config path
     */
    public String getConfigPath() {
        return StringUtils.isNotEmpty(SystemService.getEnv(KAFKACTL_CONFIG))
                ? SystemService.getEnv(KAFKACTL_CONFIG)
                : SystemService.getProperty("user.home") + "/.kafkactl/config.yml";
    }

    @Getter
    @Setter
    @Builder
    public static class ContextsProperties {
        private String name;
        private ContextProperties context;

        @Getter
        @Setter
        @Builder
        @ConfigurationProperties("context")
        public static class ContextProperties {
            private String api;
            private String userToken;
            private String namespace;
        }
    }
}
