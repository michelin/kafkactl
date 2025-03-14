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
package com.michelin.kafkactl.command;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.Output;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Users subcommand. */
@Command(
        name = "reset-password",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Reset a Kafka password.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class ResetPassword extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Parameters(description = "The user to reset password.", arity = "1")
    public String user;

    @Option(
            names = {"--execute"},
            description = "This option is mandatory to change the password")
    public boolean confirmed;

    @Option(
            names = {"-o", "--output"},
            description = "Output format (${COMPLETION-CANDIDATES}).",
            defaultValue = "table")
    public Output output;

    /**
     * Run the "users" command.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws IOException {
        String namespace = getNamespace();
        if (!confirmed) {
            commandSpec
                    .commandLine()
                    .getOut()
                    .println("You are about to change your Kafka password "
                            + "for the namespace " + namespace + ".\n"
                            + "Active connections will be killed instantly.\n\n"
                            + "To execute this operation, rerun the command with option --execute.");
            return 0;
        }

        return resourceService.resetPassword(namespace, user, output, commandSpec);
    }
}
