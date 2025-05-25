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
package com.michelin.kafkactl.command.config;

import static com.michelin.kafkactl.mixin.UnmaskTokenMixin.MASKED;
import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.CONTEXT;

import com.michelin.kafkactl.hook.HelpHook;
import com.michelin.kafkactl.mixin.UnmaskTokenMixin;
import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.property.KafkactlProperties;
import com.michelin.kafkactl.service.FormatService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Config get contexts subcommand. */
@Command(
        name = "get-contexts",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Get all contexts.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class ConfigGetContexts extends HelpHook implements Callable<Integer> {
    @Inject
    @ReflectiveAccess
    private KafkactlProperties kafkactlProperties;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Spec
    public CommandSpec commandSpec;

    @Mixin
    public UnmaskTokenMixin unmaskTokenMixin;

    @Override
    public Integer call() {
        if (kafkactlProperties.getContexts().isEmpty()) {
            commandSpec.commandLine().getOut().println("No context pre-defined.");
        } else {
            List<Resource> contexts = kafkactlProperties.getContexts().stream()
                    .map(userContext -> {
                        Map<String, Object> specs = new HashMap<>();
                        specs.put("namespace", userContext.getContext().getNamespace());
                        specs.put("api", userContext.getContext().getApi());

                        if (unmaskTokenMixin.unmaskTokens) {
                            specs.put("token", userContext.getContext().getUserToken());
                        } else {
                            specs.put("token", MASKED);
                        }

                        return Resource.builder()
                                .metadata(Metadata.builder()
                                        .name(userContext.getName())
                                        .build())
                                .spec(specs)
                                .build();
                    })
                    .toList();

            formatService.displayList(CONTEXT, contexts, TABLE, commandSpec);
        }

        return 0;
    }
}
