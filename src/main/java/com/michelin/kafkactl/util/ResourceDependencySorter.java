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

import java.util.*;

public class ResourceDependencySorter {

    public static List<String> sortResourceNamesByDependencies(
            Set<String> resourceNames, Map<String, Set<String>> dependencies) {
        LinkedHashSet<String> visited = new LinkedHashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String name : resourceNames) {
            visit(name, dependencies, visited, visiting);
        }
        return visited.stream().toList();
    }

    private static void visit(
            String resourceName,
            Map<String, Set<String>> dependencies,
            LinkedHashSet<String> visited,
            Set<String> visiting) {
        if (visited.contains(resourceName)) return;
        if (!visiting.add(resourceName)) throw new IllegalStateException("Cyclic dependency detected");
        for (String dep : dependencies.getOrDefault(resourceName, Set.of())) {
            visit(dep, dependencies, visited, visiting);
        }
        visiting.remove(resourceName);
        visited.add(resourceName);
    }
}
