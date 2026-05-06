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
package com.michelin.kafkactl.command.subjectconfig;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.SubjectCompatibility;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Subject config update subcommand. */
@Command(
        name = "update",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Update subject config.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class SubjectConfigUpdate extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Parameters(index = "0", description = "Subject name.", arity = "1")
    public String subject;

    @Option(
            names = {"--compatibility"},
            description = "Compatibility to set (${COMPLETION-CANDIDATES}).")
    public SubjectCompatibility compatibility;

    @Option(
            names = {"--alias"},
            description = "Alias to set.")
    public String alias;

    /**
     * Run the "subject" command.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws IOException {
        return resourceService
                        .updateSubjectConfig(getNamespace(), subject, compatibility, alias, commandSpec)
                        .isEmpty()
                ? 1
                : 0;
    }
}
