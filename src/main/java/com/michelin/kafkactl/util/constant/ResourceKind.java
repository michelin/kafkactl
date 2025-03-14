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
package com.michelin.kafkactl.util.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Constant kind. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceKind {
    public static final String AUTH_INFO = "AuthInfo";
    public static final String CHANGE_CONNECTOR_STATE = "ChangeConnectorState";
    public static final String CONSUMER_GROUP_RESET_OFFSET_RESPONSE = "ConsumerGroupResetOffsetsResponse";
    public static final String CONTEXT = "Context";
    public static final String CONNECTOR = "Connector";
    public static final String CONNECT_CLUSTER = "ConnectCluster";
    public static final String DELETE_RECORDS_RESPONSE = "DeleteRecordsResponse";
    public static final String KAFKA_USER_RESET_PASSWORD = "KafkaUserResetPassword";
    public static final String RESOURCE_DEFINITION = "ResourceDefinition";
    public static final String SUBJECT = "Subject";
    public static final String SCHEMA_COMPATIBILITY_STATE = "SchemaCompatibilityState";
    public static final String VAULT_RESPONSE = "VaultResponse";
}
