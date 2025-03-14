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
package com.michelin.kafkactl.client.predicates;

import com.michelin.kafkactl.model.Status;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.retry.annotation.RetryPredicate;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Optional;

/** Retry timeout predicate. */
public class RetryTimeoutPredicate implements RetryPredicate {
    /**
     * Detect when Ns4Kafka return a timeout exception (e.g. when deploying schemas, connectors)
     *
     * @param throwable the input argument
     * @return true if a retry is necessary, false otherwise
     */
    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof HttpClientResponseException response) {
            Optional<Status> statusOptional = response.getResponse().getBody(Status.class);
            if (statusOptional.isPresent()
                    && statusOptional.get().getDetails() != null
                    && !statusOptional.get().getDetails().getCauses().isEmpty()
                    && HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
                            == statusOptional.get().getCode()
                    && statusOptional.get().getDetails().getCauses().stream()
                            .anyMatch(cause -> cause.contains("Read Timeout"))) {
                System.out.println("Read timeout... retrying...");
                return true;
            }
        }
        return false;
    }
}
