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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.property.KafkactlProperties;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.filter.ClientFilterChain;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;

@ExtendWith(MockitoExtension.class)
class DynamicApiUrlClientFilterTest {
    @Mock
    private KafkactlProperties kafkactlProperties;

    @Mock
    private ClientFilterChain chain;

    private DynamicApiUrlClientFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DynamicApiUrlClientFilter();
        filter.kafkactlProperties = kafkactlProperties;
    }

    @Test
    void shouldRewriteWhenContextSwitchChangesApi() throws Exception {
        // Given: Original config has localhost:8080, but context switched to localhost:8085
        filter.originalApiUrl = "http://localhost:8080";
        when(kafkactlProperties.getApi()).thenReturn("http://localhost:8085");

        @SuppressWarnings("unchecked")
        Publisher<HttpResponse<?>> mockPublisher = mock(Publisher.class);
        doReturn(mockPublisher).when(chain).proceed(any(MutableHttpRequest.class));

        // When: Making a request
        MutableHttpRequest<?> request = HttpRequest.GET(new URI("http://localhost:8080/api/namespaces/test/topics"));
        filter.doFilter(request, chain);

        // Then: The request URI should be rewritten to localhost:8085
        @SuppressWarnings("unchecked")
        ArgumentCaptor<MutableHttpRequest<?>> captor = ArgumentCaptor.forClass(MutableHttpRequest.class);
        verify(chain).proceed(captor.capture());

        URI rewrittenUri = captor.getValue().getUri();
        assertEquals("http", rewrittenUri.getScheme());
        assertEquals("localhost", rewrittenUri.getHost());
        assertEquals(8085, rewrittenUri.getPort());
        assertEquals("/api/namespaces/test/topics", rewrittenUri.getPath());
    }

    @Test
    void shouldNotRewriteWhenNoContextSwitch() throws Exception {
        // Given: Original and current API are the same (no context switch)
        filter.originalApiUrl = "http://localhost:8080";
        when(kafkactlProperties.getApi()).thenReturn("http://localhost:8080");

        @SuppressWarnings("unchecked")
        Publisher<HttpResponse<?>> mockPublisher = mock(Publisher.class);
        doReturn(mockPublisher).when(chain).proceed(any(MutableHttpRequest.class));

        // When: Making a request
        MutableHttpRequest<?> request = HttpRequest.GET(new URI("http://localhost:8080/api/topics"));
        filter.doFilter(request, chain);

        // Then: The request should proceed unmodified
        @SuppressWarnings("unchecked")
        ArgumentCaptor<MutableHttpRequest<?>> captor = ArgumentCaptor.forClass(MutableHttpRequest.class);
        verify(chain).proceed(captor.capture());

        URI rewrittenUri = captor.getValue().getUri();
        assertEquals("http://localhost:8080/api/topics", rewrittenUri.toString());
    }

    @Test
    void shouldPreserveQueryParametersWhenRewriting() throws Exception {
        // Given: Context switch active
        filter.originalApiUrl = "http://localhost:8080";
        when(kafkactlProperties.getApi()).thenReturn("http://localhost:8085");

        @SuppressWarnings("unchecked")
        Publisher<HttpResponse<?>> mockPublisher = mock(Publisher.class);
        doReturn(mockPublisher).when(chain).proceed(any(MutableHttpRequest.class));

        // When
        MutableHttpRequest<?> request =
                HttpRequest.GET(new URI("http://localhost:8080/api/topics?dryrun=true&name=test"));
        filter.doFilter(request, chain);

        // Then: Query params should be preserved
        @SuppressWarnings("unchecked")
        ArgumentCaptor<MutableHttpRequest<?>> captor = ArgumentCaptor.forClass(MutableHttpRequest.class);
        verify(chain).proceed(captor.capture());

        URI rewrittenUri = captor.getValue().getUri();
        assertEquals("http://localhost:8085/api/topics?dryrun=true&name=test", rewrittenUri.toString());
    }

    @Test
    void shouldHandleLoginEndpointWithContextSwitch() throws Exception {
        // Given: Context switched to different host
        filter.originalApiUrl = "http://localhost:8080";
        when(kafkactlProperties.getApi()).thenReturn("http://api.example.com:8080");

        @SuppressWarnings("unchecked")
        Publisher<HttpResponse<?>> mockPublisher = mock(Publisher.class);
        doReturn(mockPublisher).when(chain).proceed(any(MutableHttpRequest.class));

        // When
        MutableHttpRequest<?> request = HttpRequest.POST(new URI("http://localhost:8080/login"), "{\"user\":\"test\"}");
        filter.doFilter(request, chain);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<MutableHttpRequest<?>> captor = ArgumentCaptor.forClass(MutableHttpRequest.class);
        verify(chain).proceed(captor.capture());

        URI rewrittenUri = captor.getValue().getUri();
        assertEquals("http://api.example.com:8080/login", rewrittenUri.toString());
    }

    @Test
    void shouldHandleNamespacedClientPathCorrectly() throws Exception {
        // Given: Context switch for NamespacedResourceClient (which has /api/namespaces/ in @Client)
        filter.originalApiUrl = "http://localhost:8080";
        when(kafkactlProperties.getApi()).thenReturn("http://localhost:8085");

        @SuppressWarnings("unchecked")
        Publisher<HttpResponse<?>> mockPublisher = mock(Publisher.class);
        doReturn(mockPublisher).when(chain).proceed(any(MutableHttpRequest.class));

        // When: Request from NamespacedResourceClient
        MutableHttpRequest<?> request =
                HttpRequest.GET(new URI("http://localhost:8080/api/namespaces/test/topics/my-topic"));
        filter.doFilter(request, chain);

        // Then: Should preserve full path including /api/namespaces/
        @SuppressWarnings("unchecked")
        ArgumentCaptor<MutableHttpRequest<?>> captor = ArgumentCaptor.forClass(MutableHttpRequest.class);
        verify(chain).proceed(captor.capture());

        URI rewrittenUri = captor.getValue().getUri();
        assertEquals("http://localhost:8085/api/namespaces/test/topics/my-topic", rewrittenUri.toString());
    }
}
