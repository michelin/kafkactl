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
package com.michelin.kafkactl.command.connectcluster;

import com.michelin.kafkactl.hook.AuthenticatedHook;
import com.michelin.kafkactl.service.ResourceService;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/** Connect cluster vault subcommand. */
@Command(
        name = "vault",
        headerHeading = "@|bold Usage|@:",
        synopsisHeading = " ",
        descriptionHeading = "%n@|bold Description|@: ",
        description = "Vault secrets for a connect cluster.",
        parameterListHeading = "%n@|bold Parameters|@:%n",
        optionListHeading = "%n@|bold Options|@:%n",
        commandListHeading = "%n@|bold Commands|@:%n",
        usageHelpAutoWidth = true)
public class ConnectClusterVault extends AuthenticatedHook {
    @Inject
    @ReflectiveAccess
    private ResourceService resourceService;

    @Parameters(
            index = "0",
            defaultValue = "",
            description = "Connect cluster name that will vault the secrets.",
            arity = "1")
    public String connectClusterName;

    @Parameters(index = "1..*", description = "Secrets to vaults separated by space.", arity = "0..*")
    public List<String> secrets;

    @Override
    public Integer onAuthSuccess() {
        String namespace = getNamespace();

        // if no parameters, list the available connect cluster to vault secrets
        if (connectClusterName.isEmpty()) {
            return resourceService.listAvailableVaultsConnectClusters(namespace, commandSpec);
        }

        // if connect cluster define but no secrets to encrypt => show error no secrets to encrypt.
        if (secrets == null) {
            throw new ParameterException(commandSpec.commandLine(), "No secrets to encrypt.");
        }

        // if connect cluster and secrets define.
        return resourceService.vaultsOnConnectClusters(namespace, connectClusterName, secrets, commandSpec);
    }
}
