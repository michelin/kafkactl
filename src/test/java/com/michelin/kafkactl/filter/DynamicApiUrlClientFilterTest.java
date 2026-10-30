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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.property.KafkactlProperties;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.filter.ClientFilterChain;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamicApiUrlClientFilterTest {
    @Mock
    private KafkactlProperties kafkactlProperties;

    @Mock
    private ClientFilterChain chain;

    @InjectMocks
    private DynamicApiUrlClientFilter filter;

    @ParameterizedTest
    @CsvSource({
        "http://localhost:8085, http://localhost:8080/api/namespaces/test/topics, http://localhost:8085/api/namespaces/test/topics",
        "http://localhost:8080, http://localhost:8080/api/topics, http://localhost:8080/api/topics",
        "http://localhost:8085, http://localhost:8080/api/topics?dryrun=true&name=test, http://localhost:8085/api/topics?dryrun=true&name=test",
        "http://localhost:8085, http://localhost:8080/api/namespaces/test/topics/my-topic, http://localhost:8085/api/namespaces/test/topics/my-topic",
        "http://api.example.com:8080, http://localhost:8080/login, http://api.example.com:8080/login"
    })
    void shouldRewriteRequestUriToCurrentApi(String currentApi, String requestUri, String expectedUri) {
        filter.originalApiUrl = "http://localhost:8080";
        when(kafkactlProperties.getApi()).thenReturn(currentApi);

        MutableHttpRequest<?> request = HttpRequest.GET(requestUri);

        filter.doFilter(request, chain);

        verify(chain).proceed(request);
        assertEquals(expectedUri, request.getUri().toString());
    }
}
