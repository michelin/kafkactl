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
package com.michelin.kafkactl.command.auth;

import com.michelin.kafkactl.hook.HelpHook;
import com.michelin.kafkactl.mixin.VerboseMixin;
import com.michelin.kafkactl.service.LoginService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Auth renew subcommand. */
@Command(
        name = "renew",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Renew the JWT token.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class AuthRenew extends HelpHook implements Callable<Integer> {
    @Inject
    @ReflectiveAccess
    private LoginService loginService;

    @Mixin
    public VerboseMixin verboseMixin;

    @Spec
    public CommandSpec commandSpec;

    @Override
    public Integer call() throws IOException {
        loginService.deleteJwtFile();
        if (loginService.doAuthenticate(commandSpec, verboseMixin.verbose)) {
            commandSpec.commandLine().getOut().println("JWT renewed successfully.");
            return 0;
        }

        commandSpec.commandLine().getErr().println("Failed to renew JWT.");
        return 1;
    }
}
