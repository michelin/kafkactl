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

import com.michelin.kafkactl.property.KafkactlProperties;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/** Config service. */
@Singleton
public class ConfigService {
    @Inject
    @ReflectiveAccess
    private KafkactlProperties kafkactlProperties;

    @Inject
    @ReflectiveAccess
    private LoginService loginService;

    /**
     * Return the name of the current context according to the current api, namespace and token properties.
     *
     * @return The current context name
     */
    public String getCurrentContextName() {
        return kafkactlProperties.getContexts().stream()
                .filter(context -> context.getContext().getApi().equals(kafkactlProperties.getApi())
                        && context.getContext().getNamespace().equals(kafkactlProperties.getCurrentNamespace())
                        && context.getContext().getUserToken().equals(kafkactlProperties.getUserToken()))
                .findFirst()
                .map(KafkactlProperties.ContextsProperties::getName)
                .orElse(null);
    }

    /**
     * Get the current context infos if it exists.
     *
     * @return The current context
     */
    public Optional<KafkactlProperties.ContextsProperties> getContextByName(String name) {
        return kafkactlProperties.getContexts().stream()
                .filter(context -> context.getName().equals(name))
                .findFirst();
    }

    /**
     * Update the current configuration context with the given new context.
     *
     * @param contextPropertiesToSet The context to set
     * @throws IOException Any exception during file writing
     */
    public void updateConfigurationContext(KafkactlProperties.ContextsProperties contextPropertiesToSet)
            throws IOException {
        Yaml yaml = new Yaml();
        File initialFile = new File(kafkactlProperties.getConfigPath());
        InputStream targetStream = new FileInputStream(initialFile);
        Map<String, LinkedHashMap<String, Object>> rootNodeConfig = yaml.load(targetStream);

        LinkedHashMap<String, Object> kafkactlNodeConfig = rootNodeConfig.get("kafkactl");
        kafkactlNodeConfig.put(
                "current-namespace", contextPropertiesToSet.getContext().getNamespace());
        kafkactlNodeConfig.put("api", contextPropertiesToSet.getContext().getApi());
        kafkactlNodeConfig.put("user-token", contextPropertiesToSet.getContext().getUserToken());

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yamlMapper = new Yaml(options);
        FileWriter writer = new FileWriter(kafkactlProperties.getConfigPath());
        yamlMapper.dump(rootNodeConfig, writer);

        loginService.deleteJwtFile();
    }

    /**
     * Check if the current context is valid.
     *
     * @return True if the current context is valid, false otherwise
     */
    public boolean isCurrentContextValid() {
        return kafkactlProperties.getApi() != null
                && kafkactlProperties.getCurrentNamespace() != null
                && kafkactlProperties.getUserToken() != null;
    }
}
