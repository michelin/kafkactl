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

import com.michelin.kafkactl.hook.HelpHook;
import com.michelin.kafkactl.property.KafkactlProperties;
import com.michelin.kafkactl.service.ConfigService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Config use context subcommand. */
@Command(
        name = "use-context",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Use a context.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class ConfigUseContext extends HelpHook implements Callable<Integer> {
    @Inject
    @ReflectiveAccess
    private KafkactlProperties kafkactlProperties;

    @Inject
    @ReflectiveAccess
    private ConfigService configService;

    @Parameters(index = "0", defaultValue = "", description = "Context to use.", arity = "1")
    public String context;

    @Spec
    public CommandSpec commandSpec;

    @Override
    public Integer call() throws IOException {
        if (kafkactlProperties.getContexts().isEmpty()) {
            commandSpec.commandLine().getOut().println("No context pre-defined.");
            return 0;
        }

        Optional<KafkactlProperties.ContextsProperties> optionalContextToSet = configService.getContextByName(context);
        if (optionalContextToSet.isEmpty()) {
            commandSpec.commandLine().getErr().println("No context exists with the name \"" + context + "\".");
            return 1;
        }

        KafkactlProperties.ContextsProperties contextPropertiesToSet = optionalContextToSet.get();
        configService.updateConfigurationContext(contextPropertiesToSet);
        commandSpec.commandLine().getOut().println("Switched to context \"" + context + "\".");

        return 0;
    }
}
