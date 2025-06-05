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
package com.michelin.kafkactl.service.resource;

import static com.michelin.kafkactl.service.ResourceService.SCHEMA;
import static com.michelin.kafkactl.service.ResourceService.SCHEMA_FILE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import com.michelin.kafkactl.service.FileService;
import com.michelin.kafkactl.service.ResourceService;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ResourceServicePrepareResourcesTest {

    record SchemaData(String schemaContent, List<String> references) {}

    @InjectMocks
    private ResourceService resourceService;

    @Spy
    private FileService fileService;

    @Mock
    private CommandLine.Model.CommandSpec commandSpec;

    private static Map<String, Resource> buildResources(Map<String, SchemaData> schemasMap) {
        Map<String, Resource> resourceMap = new HashMap<>();
        for (Map.Entry<String, SchemaData> entry : schemasMap.entrySet()) {
            String schemaContent = entry.getValue().schemaContent;
            String schemaName = entry.getKey();
            Map<String, Object> spec = new HashMap<>(Map.of(SCHEMA, schemaContent));
            var referencesMap = new ArrayList<Map<String, String>>();
            for (String ref : entry.getValue().references) {
                referencesMap.add(Map.of("name", ref));
            }
            spec.put("references", referencesMap);
            Resource res = Resource.builder()
                    .kind("Schema")
                    .metadata(Metadata.builder().name(schemaName).build())
                    .spec(spec)
                    .build();
            resourceMap.put(schemaName, res);
        }
        return resourceMap;
    }

    @Test
    void shouldEnrichSchemaFromFileWhenPrepareResources() throws Exception {

        File tempSchema = File.createTempFile("test-schema", ".avsc");
        String schemaContent = "{\"type\":\"record\",\"name\":\"Test\"}";
        Files.writeString(tempSchema.toPath(), schemaContent);

        Map<String, Object> spec = new HashMap<>();
        spec.put(SCHEMA_FILE, tempSchema.getAbsolutePath());

        Resource schemaResource = Resource.builder()
                .kind("Schema")
                .metadata(Metadata.builder().name("test").build())
                .spec(spec)
                .build();

        List<Resource> resources = new ArrayList<>(List.of(schemaResource));

        resourceService.prepareResources(resources, commandSpec);

        assertEquals(schemaContent, schemaResource.getSpec().get("schema"));

        tempSchema.delete();
    }

    @Test
    void shouldHandleFileNotFoundWhenPrepareResources() {
        Map<String, Object> spec = new HashMap<>(Map.of(SCHEMA_FILE, "not-exist.avsc"));

        Resource schemaResource = Resource.builder()
                .kind("Schema")
                .metadata(Metadata.builder().name("test").build())
                .spec(spec)
                .build();

        when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));

        Exception ex = assertThrows(
                CommandLine.ParameterException.class,
                () -> resourceService.prepareResources(List.of(schemaResource), commandSpec));

        assertEquals(
                "Cannot open schema file not-exist.avsc. Schema path must be relative to the CLI.", ex.getMessage());
        assertNull(schemaResource.getSpec().get("schema"));
    }

    @Test
    void shouldEnrichSchemasWithUnionDependencyWhenPrepareResources() {
        String schema1 = "{\"type\":\"record\",\"name\":\"Test1\",\"namespace\":\"com.example.one\"}";
        String schema2 = "{\"type\":\"record\",\"name\":\"Test2\",\"namespace\":\"com.example.two\"}";
        String schema3 =
                """
                                {
                                  "type": "record",
                                  "name": "Test3",
                                  "namespace": "com.example.three",
                                  "fields": [
                                    {"name": "ref", "type": "com.example.one.Test1"}
                                  ]
                                }
                                """
                        .replace("\n", "")
                        .replace("  ", "");

        String schemaUnion =
                """
                                [
                                        "com.example.one.Test1",
                                        "com.example.two.Test2",
                                        "com.example.three.Test3"
                                      ]
                                """
                        .replace("\n", "")
                        .replace("  ", "");

        Map<String, SchemaData> schemasMap = new HashMap<>(Map.of(
                "schemaUnion",
                new SchemaData(
                        schemaUnion,
                        List.of("com.example.one.Test1", "com.example.two.Test2", "com.example.three.Test3")),
                "schema1",
                new SchemaData(schema1, List.of()),
                "schema2",
                new SchemaData(schema2, List.of()),
                "schema3",
                new SchemaData(schema3, List.of("com.example.one.Test1"))));

        Map<String, Resource> resourceMap = buildResources(schemasMap);

        List<Resource> resources = new ArrayList<>(List.of(
                resourceMap.get("schema3"), resourceMap.get("schema2"),
                resourceMap.get("schema1"), resourceMap.get("schemaUnion")));

        List<Resource> sortedResources = resourceService.prepareResources(resources, commandSpec);

        List<String> names =
                sortedResources.stream().map(r -> r.getMetadata().getName()).toList();

        assertTrue(names.indexOf("schema1") < names.indexOf("schema3"));
        for (var schemaName : List.of("schema1", "schema2", "schema3")) {
            assertTrue(names.indexOf("schemaUnion") > names.indexOf(schemaName));
        }
    }

    @Test
    void shouldSortResourcesByDependenciesWhenPrepareResources() {
        String schema1 = "{\"type\":\"record\",\"name\":\"Test1\",\"namespace\":\"com.example.one\"}";
        String schema2 =
                """
                                {
                                  "type": "record",
                                  "name": "Test2",
                                  "namespace": "com.example.two",
                                  "fields": [
                                    {"name": "ref", "type": "com.example.one.Test1"}
                                  ]
                                }
                                """
                        .replace("\n", "")
                        .replace("  ", "");
        // Schéma qui dépend de Test2
        String schema3 =
                """
                                {
                                  "type": "record",
                                  "name": "Test3",
                                  "namespace": "com.example.three",
                                  "fields": [
                                    {"name": "ref", "type": "com.example.two.Test2"}
                                  ]
                                }
                                """
                        .replace("\n", "")
                        .replace("  ", "");

        Map<String, SchemaData> schemasMap = new HashMap<>(Map.of(
                "schema1", new SchemaData(schema1, List.of()),
                "schema2", new SchemaData(schema2, List.of("com.example.one.Test1")),
                "schema3", new SchemaData(schema3, List.of("com.example.two.Test2"))));

        Map<String, Resource> rMap = buildResources(schemasMap);

        List<Resource> resources =
                new ArrayList<>(List.of(rMap.get("schema3"), rMap.get("schema2"), rMap.get("schema1")));

        var sortedResources = resourceService.prepareResources(resources, commandSpec);

        assertEquals(
                List.of("schema1", "schema2", "schema3"),
                sortedResources.stream().map(r -> r.getMetadata().getName()).toList());
    }

    @Test
    void shouldHandleNestedFieldsWithDeepDependencyWhenPrepareResources() {
        String schemaLeaf =
                """
                                {
                                  "type": "record",
                                  "name": "Leaf",
                                  "namespace": "com.example.leaf"
                                }"""
                        .replace("\n", "")
                        .replace("  ", "");

        String schemaNested =
                """
                                {
                                  "type": "record",
                                  "name": "Nested",
                                  "namespace": "com.example.nested",
                                  "fields": [
                                    {
                                      "name": "level1",
                                      "type": {
                                        "type": "record",
                                        "name": "Level1",
                                        "fields": [
                                          {
                                            "name": "level2",
                                            "type": {
                                              "type": "record",
                                              "name": "Level2",
                                              "fields": [
                                                {
                                                  "name": "leafRef",
                                                  "type": "com.example.leaf.Leaf"
                                                }
                                              ]
                                            }
                                          }
                                        ]
                                      }
                                    }
                                  ]
                                }"""
                        .replace("\n", "")
                        .replace("  ", "");

        Map<String, SchemaData> schemasMap = new HashMap<>(Map.of(
                "schemaLeaf", new SchemaData(schemaLeaf, List.of()),
                "schemaNested", new SchemaData(schemaNested, List.of("com.example.leaf.Leaf"))));

        Map<String, Resource> rMap = buildResources(schemasMap);

        List<Resource> resources = new ArrayList<>(List.of(rMap.get("schemaNested"), rMap.get("schemaLeaf")));
        var sortedResources = resourceService.prepareResources(resources, commandSpec);

        assertEquals(
                List.of("schemaLeaf", "schemaNested"),
                sortedResources.stream().map(r -> r.getMetadata().getName()).toList());
    }

    @Test
    void shouldAddNamespacesBeforeSchemasWhenPrepareResource() {
        ResourceService resourceService = new ResourceService();

        String schemaContent = "{\"type\":\"record\",\"name\":\"Test1\",\"namespace\":\"com.example\"}";
        String schemaContent2 = "{\"type\":\"record\",\"name\":\"Test2\",\"namespace\":\"com.example\"}";
        Resource schema1 = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("schema1").build())
                .spec(new HashMap(Map.of(SCHEMA, schemaContent)))
                .build();

        Resource namespace = Resource.builder()
                .kind("Namespace")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("ns1").build())
                .spec(Map.of())
                .build();

        Resource schema2 = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("schema2").build())
                .spec(new HashMap(Map.of(SCHEMA, schemaContent2)))
                .build();

        List<Resource> input = List.of(schema1, namespace, schema2);

        List<Resource> result = resourceService.prepareResources(input, commandSpec);

        assertEquals("Namespace", result.get(0).getKind());
        assertEquals("Schema", result.get(1).getKind());
        assertEquals("Schema", result.get(2).getKind());
    }

    @Test
    void shouldOrderResourcesFromYamlFileWithNamespacesACLsRoleBindingsFirst() {
        File yamlFile = new File("src/test/resources/resource_service/resources-unordered.yml");
        List<Resource> resources = resourceService.parseResources(java.util.Optional.of(yamlFile), false, commandSpec);

        List<Resource> sorted = resourceService.prepareResources(resources, commandSpec);

        List<String> expectedOrder = List.of(
                "demo",
                "myRoleBinding1",
                "myRoleBinding2",
                "acl-topic-schema",
                "acl-topic-demo",
                "demoPrefix.topic_64-demo.Car",
                "demoPrefix.topic_64-demo.User",
                "demoPrefix.topic_64-value",
                "demoPrefix.topic_64");
        List<String> actualOrder =
                sorted.stream().map(r -> r.getMetadata().getName()).toList();

        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    void shouldKeepResourcesOrderedWhenAlreadySorted() {
        File yamlFile = new File("src/test/resources/resource_service/resources-in-order.yml");
        List<Resource> resources = resourceService.parseResources(java.util.Optional.of(yamlFile), false, commandSpec);

        List<Resource> sorted = resourceService.prepareResources(resources, commandSpec);

        List<String> expectedOrder = List.of(
                "demo", "myRoleBinding1", "acl-topic-schema", "demoPrefix.topic_64-demo.Car", "demoPrefix.topic_64");
        List<String> actualOrder =
                sorted.stream().map(r -> r.getMetadata().getName()).toList();

        assertEquals(expectedOrder, actualOrder);
    }
}
