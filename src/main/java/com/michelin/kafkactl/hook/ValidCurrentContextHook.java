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

import com.michelin.kafkactl.command.config.Config;
import com.michelin.kafkactl.command.config.ConfigUseContext;
import com.michelin.kafkactl.mixin.ContextMixin;
import com.michelin.kafkactl.service.ConfigService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Command hook to check if the current context is valid. */
@Command
public abstract class ValidCurrentContextHook extends HelpHook implements Callable<Integer> {
    @Inject
    @ReflectiveAccess
    protected ConfigService configService;

    @Spec
    protected CommandSpec commandSpec;

    @Mixin
    public ContextMixin contextMixin;

    @Override
    public Integer call() throws Exception {
        // Apply context from command-line option if specified
        if (contextMixin.optionalContext.isPresent()) {
            String contextName = contextMixin.optionalContext.get();
            if (!configService.applyContextByName(contextName)) {
                commandSpec.commandLine().getErr().println("No context exists with the name \"" + contextName + "\".");
                return 1;
            }
        }

        if (!configService.isCurrentContextValid()) {
            CommandLine configSubcommand = new CommandLine(new Config());
            CommandLine configUseContextSubcommand = new CommandLine(new ConfigUseContext());
            commandSpec
                    .commandLine()
                    .getErr()
                    .println("No valid current context found. Use \"kafkactl "
                            + configSubcommand.getCommandName() + " "
                            + configUseContextSubcommand.getCommandName() + "\" to set a valid context.");
            return 1;
        }
        return onContextValid();
    }

    public abstract Integer onContextValid() throws IOException;
}
