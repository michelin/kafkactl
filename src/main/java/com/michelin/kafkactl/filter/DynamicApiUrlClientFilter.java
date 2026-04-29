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
package com.michelin.kafkactl.filter;

import com.michelin.kafkactl.property.KafkactlProperties;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import org.reactivestreams.Publisher;

/**
 * HTTP client filter that dynamically rewrites request URIs when context switching changes the API URL.
 *
 * <p>This filter compares the current API URL from KafkactlProperties (which reflects context switches via
 * -c/--context) with the original startup configuration. If they differ, it rewrites the request URI to use the current
 * context's API.
 *
 * <p>This allows the @Client("${kafkactl.api}") annotation to work normally while still supporting runtime context
 * switching.
 */
@Filter(Filter.MATCH_ALL_PATTERN)
@Requires(property = "kafkactl.api")
public class DynamicApiUrlClientFilter implements HttpClientFilter {

    @Inject
    @ReflectiveAccess
    KafkactlProperties kafkactlProperties; // Package-private for testing

    @Nullable @Property(name = "kafkactl.api")
    @ReflectiveAccess
    String originalApiUrl; // The API URL from config file (at startup)

    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        // Get the current API URL from properties (may have been changed by context switching)
        String currentApiUrl = kafkactlProperties.getApi();

        if (currentApiUrl == null || currentApiUrl.isEmpty()) {
            // No API URL configured, proceed without modification
            return chain.proceed(request);
        }

        // Check if context switching has changed the API URL
        boolean hasContextOverride = originalApiUrl != null && !originalApiUrl.equals(currentApiUrl);

        if (!hasContextOverride) {
            // No override needed - current URL matches original, proceed normally
            return chain.proceed(request);
        }

        // Context has been switched - need to rewrite the URL
        try {
            URI originalUri = request.getUri();

            // Parse the current (overridden) API URL
            URI currentBaseUri = new URI(currentApiUrl);

            // Replace the scheme/host/port from original with current, preserving path/query
            String originalPath = originalUri.getPath();

            URI newUri = new URI(
                    currentBaseUri.getScheme(),
                    currentBaseUri.getUserInfo(),
                    currentBaseUri.getHost(),
                    currentBaseUri.getPort(),
                    originalPath,
                    originalUri.getQuery(),
                    originalUri.getFragment());

            // Update the request URI with the rewritten URL
            request.uri(newUri);

        } catch (URISyntaxException e) {
            throw new RuntimeException(
                    "Failed to rewrite request URI. Original: " + originalApiUrl + ", Current: " + currentApiUrl, e);
        }

        return chain.proceed(request);
    }
}
