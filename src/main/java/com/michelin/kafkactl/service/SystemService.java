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
package com.michelin.kafkactl.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** The system service. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemService {
    /**
     * Get a system environment variable by name.
     *
     * @param name The var name
     * @return The system environment variable
     */
    public static String getEnv(String name) {
        return System.getenv(name);
    }

    /**
     * Get a system property by name.
     *
     * @param name The property name
     * @return The system property name
     */
    public static String getProperty(String name) {
        return System.getProperty(name);
    }

    /**
     * Set a system property.
     *
     * @param key The name
     * @param value The value
     */
    public static void setProperty(String key, String value) {
        System.setProperty(key, value);
    }
}
