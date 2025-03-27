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
package com.michelin.kafkactl.hook;

import com.michelin.kafkactl.config.KafkactlConfig;
import com.michelin.kafkactl.mixin.NamespaceMixin;
import com.michelin.kafkactl.mixin.VerboseMixin;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.ApiResourcesService;
import com.michelin.kafkactl.service.LoginService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.ParameterException;

/** Authenticated command. */
@Command
public abstract class AuthenticatedHook extends ValidCurrentContextHook {
    @Inject
    @ReflectiveAccess
    protected LoginService loginService;

    @Inject
    @ReflectiveAccess
    protected ApiResourcesService apiResourcesService;

    @Inject
    @ReflectiveAccess
    protected KafkactlConfig kafkactlConfig;

    @Mixin
    public NamespaceMixin namespaceMixin;

    @Mixin
    public VerboseMixin verboseMixin;

    @Override
    public Integer onContextValid() throws IOException {
        if (!loginService.doAuthenticate(commandSpec, verboseMixin.verbose)) {
            return 1;
        }

        return onAuthSuccess();
    }

    /**
     * Gets the current namespace.
     *
     * @return The current namespace
     */
    protected String getNamespace() {
        return namespaceMixin.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());
    }

    /**
     * Validate the namespace does not mismatch between Kafkactl and YAML document.
     *
     * @param resources The resources
     */
    protected void validateNamespace(List<Resource> resources) {
        List<Resource> namespaceMismatch = resources.stream()
                .filter(resource -> resource.getMetadata().getNamespace() != null
                        && !resource.getMetadata().getNamespace().equals(getNamespace()))
                .toList();

        if (!namespaceMismatch.isEmpty()) {
            String invalid = namespaceMismatch.stream()
                    .map(resource -> "\"" + resource.getKind() + "/"
                            + resource.getMetadata().getName() + "\"")
                    .distinct()
                    .collect(Collectors.joining(", "));
            throw new ParameterException(
                    commandSpec.commandLine(),
                    "Namespace mismatch between Kafkactl configuration and YAML resource(s): " + invalid + ".");
        }
    }

    /**
     * Run after authentication success.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    public abstract Integer onAuthSuccess() throws IOException;
}
