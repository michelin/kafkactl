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

import com.michelin.kafkactl.config.KafkactlConfig;
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
    private KafkactlConfig kafkactlConfig;

    @Inject
    @ReflectiveAccess
    private LoginService loginService;

    /**
     * Return the name of the current context according to the current api, namespace and token properties.
     *
     * @return The current context name
     */
    public String getCurrentContextName() {
        return kafkactlConfig.getContexts().stream()
                .filter(context -> context.getDefinition().getApi().equals(kafkactlConfig.getApi())
                        && context.getDefinition().getNamespace().equals(kafkactlConfig.getCurrentNamespace())
                        && context.getDefinition().getUserToken().equals(kafkactlConfig.getUserToken()))
                .findFirst()
                .map(KafkactlConfig.Context::getName)
                .orElse(null);
    }

    /**
     * Get the current context infos if it exists.
     *
     * @return The current context
     */
    public Optional<KafkactlConfig.Context> getContextByName(String name) {
        return kafkactlConfig.getContexts().stream()
                .filter(context -> context.getName().equals(name))
                .findFirst();
    }

    /**
     * Update the current configuration context with the given new context.
     *
     * @param contextToSet The context to set
     * @throws IOException Any exception during file writing
     */
    public void updateConfigurationContext(KafkactlConfig.Context contextToSet) throws IOException {
        Yaml yaml = new Yaml();
        File initialFile = new File(kafkactlConfig.getConfigPath());
        InputStream targetStream = new FileInputStream(initialFile);
        Map<String, LinkedHashMap<String, Object>> rootNodeConfig = yaml.load(targetStream);

        LinkedHashMap<String, Object> kafkactlNodeConfig = rootNodeConfig.get("kafkactl");
        kafkactlNodeConfig.put("current-namespace", contextToSet.getDefinition().getNamespace());
        kafkactlNodeConfig.put("api", contextToSet.getDefinition().getApi());
        kafkactlNodeConfig.put("user-token", contextToSet.getDefinition().getUserToken());

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yamlMapper = new Yaml(options);
        FileWriter writer = new FileWriter(kafkactlConfig.getConfigPath());
        yamlMapper.dump(rootNodeConfig, writer);

        loginService.deleteJwtFile();
    }

    /**
     * Check if the current context is valid.
     *
     * @return True if the current context is valid, false otherwise
     */
    public boolean isCurrentContextValid() {
        return kafkactlConfig.getApi() != null
                && kafkactlConfig.getCurrentNamespace() != null
                && kafkactlConfig.getUserToken() != null;
    }
}
