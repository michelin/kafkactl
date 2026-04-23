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
package com.michelin.kafkactl.model;

/** Delete mode. */
public enum DeleteMode {
    STANDARD(false, false),
    FORCE(true, false),
    CASCADE(false, true),
    FORCE_CASCADE(true, true);

    private final boolean force;
    private final boolean cascade;

    DeleteMode(boolean force, boolean cascade) {
        this.force = force;
        this.cascade = cascade;
    }

    public boolean force() {
        return force;
    }

    public boolean cascade() {
        return cascade;
    }

    public static DeleteMode of(boolean force, boolean cascade) {
        if (force && cascade) {
            return FORCE_CASCADE;
        }
        if (force) {
            return FORCE;
        }
        if (cascade) {
            return CASCADE;
        }
        return STANDARD;
    }
}