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

import io.micronaut.core.annotation.ReflectiveAccess;
import java.util.concurrent.Callable;
import picocli.AutoComplete;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Completion subcommand to generate shell completion scripts. */
@Command(
        name = "completion",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Generate shell completion scripts.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true,
        mixinStandardHelpOptions = true)
@ReflectiveAccess
public class Completion implements Callable<Integer> {
    @Spec
    CommandSpec commandSpec;

    /**
     * Run the completion command.
     *
     * @return The command return code
     */
    @Override
    public Integer call() {
        try {
            String script = AutoComplete.bash("kafkactl", commandSpec.root().commandLine());
            commandSpec.commandLine().getOut().println(script);
            return 0;
        } catch (Exception e) {
            commandSpec
                    .commandLine()
                    .getErr()
                    .println("An error occurred while generating completion script: " + e.getMessage());
            return 1;
        }
    }
}
