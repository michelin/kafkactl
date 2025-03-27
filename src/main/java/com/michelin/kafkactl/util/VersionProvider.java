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
package com.michelin.kafkactl.util;

import com.michelin.kafkactl.config.KafkactlConfig;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine.IVersionProvider;

/** Version provider. */
@Singleton
public class VersionProvider implements IVersionProvider {
    @Inject
    @ReflectiveAccess
    private KafkactlConfig kafkactlConfig;

    @Override
    public String[] getVersion() {
        return new String[] {"Version " + kafkactlConfig.getVersion()};
    }
}
