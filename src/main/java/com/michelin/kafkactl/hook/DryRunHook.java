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

import java.io.IOException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Dry run abstract command. */
@Command
public abstract class DryRunHook extends AuthenticatedHook {
    @Option(
            names = {"--dry-run"},
            description = "Does not persist resources. Validate only.")
    public boolean dryRun;

    @Override
    public Integer onContextValid() throws IOException {
        if (dryRun) {
            commandSpec.commandLine().getOut().println("Dry run execution.");
        }

        return super.onContextValid();
    }
}
