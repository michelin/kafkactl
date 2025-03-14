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

import static com.michelin.kafkactl.model.Output.TABLE;
import static com.michelin.kafkactl.util.constant.ResourceKind.SCHEMA_COMPATIBILITY_STATE;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.model.SchemaCompatibility;
import com.michelin.kafkactl.service.FormatService;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/** Schema subcommand. */
@Command(
        name = "schema",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Interact with schemas.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class Schema extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Inject
    @ReflectiveAccess
    private FormatService formatService;

    @Parameters(index = "0", description = "Compatibility to set (${COMPLETION-CANDIDATES}).", arity = "1")
    public SchemaCompatibility compatibility;

    @Parameters(index = "1..*", description = "Subject names separated by space.", arity = "1..*")
    public List<String> subjects;

    /**
     * Run the "reset-offsets" command.
     *
     * @return The command return code
     * @throws IOException Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws IOException {
        List<Resource> updatedSchemas = subjects.stream()
                .map(subject ->
                        resourceService.changeSchemaCompatibility(getNamespace(), subject, compatibility, commandSpec))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (!updatedSchemas.isEmpty()) {
            formatService.displayList(SCHEMA_COMPATIBILITY_STATE, updatedSchemas, TABLE, commandSpec);
            return 0;
        }

        return 1;
    }
}
