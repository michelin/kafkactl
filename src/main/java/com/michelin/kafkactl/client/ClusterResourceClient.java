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
package com.michelin.kafkactl.client;

import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Resource;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.micronaut.retry.annotation.Retryable;
import java.util.List;
import java.util.Map;

/** Cluster resource client. */
@Client("${kafkactl.api}")
public interface ClusterResourceClient {
    /**
     * Login.
     *
     * @param request The username and password information
     * @return The JWT token
     */
    @Post("/login")
    @Retryable(
        delay = "${kafkactl.retry.delay}",
        attempts = "${kafkactl.retry.attempt}",
        multiplier = "${kafkactl.retry.multiplier}",
        includes = ReadTimeoutException.class)
    BearerAccessRefreshToken login(@Body UsernameAndPasswordRequest request);

    /**
     * Get JWT token information.
     *
     * @param token The auth token
     * @return The JWT token information
     */
    @Get("/token_info")
    UserInfoResponse tokenInfo(@Header("Authorization") String token);

    /**
     * List all resources kinds.
     *
     * @param token The auth token
     * @return The list of resources kinds
     */
    @Get("/api-resources")
    List<ApiResource> listResourceDefinitions(@Header("Authorization") String token);

    /**
     * Delete a given resource.
     *
     * @param token The auth token
     * @param kind The kind of resource
     * @param name The name of the resource
     * @param dryrun Is dry-run mode or not?
     * @return The delete response
     */
    @Delete("/api/{kind}{?name,dryrun}")
    HttpResponse<List<Resource>> delete(
            @Header("Authorization") String token, String kind, @QueryValue String name, @QueryValue boolean dryrun);

    /**
     * Apply a given resource.
     *
     * @param token The auth token
     * @param kind The kind of resource
     * @param resource The resource to apply
     * @param dryrun Is dry-run mode or not?
     * @return The resource
     */
    @Post("/api/{kind}{?dryrun}")
    HttpResponse<Resource> apply(
            @Header("Authorization") String token, String kind, @Body Resource resource, @QueryValue boolean dryrun);

    /**
     * List all resources.
     *
     * @param token The auth token
     * @param kind The kind of resource
     * @param search The query parameters mapping
     * @return The list of resources
     */
    @Get("/api/{kind}{?search*}")
    List<Resource> list(@Header("Authorization") String token, String kind, @QueryValue Map<String, String> search);

    /**
     * Get a resource.
     *
     * @param token The auth token
     * @param kind The kind of resource
     * @param resource The name of the resource
     * @return The resource
     */
    @Get("/api/{kind}/{resource}")
    HttpResponse<Resource> get(@Header("Authorization") String token, String kind, String resource);
}
