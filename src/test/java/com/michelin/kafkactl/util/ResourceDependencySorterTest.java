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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResourceDependencySorterTest {

    @Test
    void shouldSortStringsByDependenciesWhenSortResourceNamesByDependencies() {
        Set<String> resourceNames = Set.of("A", "B", "C");
        Map<String, Set<String>> deps = Map.of(
                "A", Set.of(),
                "B", Set.of("A"),
                "C", Set.of("B", "A"));
        List<String> sorted = ResourceDependencySorter.sortResourceNamesByDependencies(resourceNames, deps);
        assertEquals(List.of("A", "B", "C"), sorted);
    }

    @Test
    void shouldThrowOnCycleWhenSortResourceNamesByDependencies() {
        Set<String> resourceNames = Set.of("A");
        Map<String, Set<String>> deps = Map.of("A", Set.of("A"));
        assertThrows(
                IllegalStateException.class,
                () -> ResourceDependencySorter.sortResourceNamesByDependencies(resourceNames, deps));
    }
}
