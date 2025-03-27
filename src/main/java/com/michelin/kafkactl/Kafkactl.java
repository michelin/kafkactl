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
package com.michelin.kafkactl;

import static com.michelin.kafkactl.config.KafkactlConfig.KAFKACTL_CONFIG;

import com.michelin.kafkactl.command.ApiResources;
import com.michelin.kafkactl.command.Apply;
import com.michelin.kafkactl.command.Connector;
import com.michelin.kafkactl.command.Delete;
import com.michelin.kafkactl.command.DeleteRecords;
import com.michelin.kafkactl.command.Diff;
import com.michelin.kafkactl.command.Get;
import com.michelin.kafkactl.command.Import;
import com.michelin.kafkactl.command.ResetOffsets;
import com.michelin.kafkactl.command.ResetPassword;
import com.michelin.kafkactl.command.Schema;
import com.michelin.kafkactl.command.auth.Auth;
import com.michelin.kafkactl.command.config.Config;
import com.michelin.kafkactl.command.connectcluster.ConnectCluster;
import com.michelin.kafkactl.service.SystemService;
import com.michelin.kafkactl.util.VersionProvider;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.core.util.StringUtils;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Kafkactl command. */
@Command(
        name = "kafkactl",
        subcommands = {
            ApiResources.class,
            Apply.class,
            Auth.class,
            Config.class,
            ConnectCluster.class,
            Connector.class,
            DeleteRecords.class,
            Delete.class,
            Diff.class,
            Get.class,
            Import.class,
            ResetOffsets.class,
            Schema.class,
            ResetPassword.class
        },
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "These are common Kafkactl commands.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true,
        versionProvider = VersionProvider.class,
        mixinStandardHelpOptions = true)
public class Kafkactl implements Callable<Integer> {
    @Spec
    public CommandSpec commandSpec;

    /**
     * Main Micronaut method There are 3 ways to configure kafkactl: - Setup config file in $HOME/.kafkactl/config.yml -
     * Setup config file anywhere and set KAFKACTL_CONFIG=/path/to/config.yml - No file but environment variables
     * instead
     *
     * @param args Input arguments
     */
    public static void main(String[] args) {
        if (System.getenv().keySet().stream().noneMatch(s -> s.startsWith("KAFKACTL_"))) {
            SystemService.setProperty(
                    "micronaut.config.files", SystemService.getProperty("user.home") + "/.kafkactl/config.yml");
        }

        if (StringUtils.isNotEmpty(SystemService.getEnv(KAFKACTL_CONFIG))) {
            SystemService.setProperty("micronaut.config.files", SystemService.getEnv(KAFKACTL_CONFIG));
        }

        int exitCode = PicocliRunner.execute(Kafkactl.class, args);
        System.exit(exitCode);
    }

    /**
     * Run the "kafkactl" command.
     *
     * @return The command return code
     */
    public Integer call() {
        commandSpec.commandLine().getOut().println(new CommandLine(this).getUsageMessage());
        return 0;
    }
}
