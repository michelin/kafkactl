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
package com.michelin.kafkactl.service;

import static com.michelin.kafkactl.service.ResourceService.SCHEMA_FILE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class ResourceServicePrepareResourcesTest {

    record SchemaData(String schemaContent, File schemaTempFile, List<String> references) {}

    private ResourceService resourceService;
    private CommandLine.Model.CommandSpec commandSpec;

    @BeforeEach
    void setUp() {
        resourceService = new ResourceService();
        commandSpec = mock(CommandLine.Model.CommandSpec.class);
        CommandLine commandLine = mock(CommandLine.class);
        when(commandSpec.commandLine()).thenReturn(commandLine);
    }

    /**
     * Build a list of resources from a map of schema data.
     *
     * @param filesMap A map where the key is the schema name and the value is the SchemaData containing content and
     *     temp file
     * @return A list of Resource objects
     * @throws IOException If there is an error writing to the temporary files
     */
    private static Map<String, Resource> buildResources(Map<String, SchemaData> filesMap) throws IOException {
        Map<String, Resource> resourceMap = new HashMap<>();
        for (Map.Entry<String, SchemaData> entry : filesMap.entrySet()) {
            File file = entry.getValue().schemaTempFile;
            String schemaContent = entry.getValue().schemaContent;
            String schemaName = entry.getKey();
            Files.writeString(file.toPath(), schemaContent);
            Map<String, Object> spec = new HashMap<>(Map.of(SCHEMA_FILE, file.getAbsolutePath()));
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
    void prepareResources_shouldEnrichSchemaFromFile() throws Exception {

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
    void prepareResources_shouldHandleFileNotFound() {
        String fakePath = "not-exist.avsc";
        Map<String, Object> spec = new HashMap<>();
        spec.put(SCHEMA_FILE, fakePath);

        Resource schemaResource = Resource.builder()
                .kind("Schema")
                .metadata(Metadata.builder().name("test").build())
                .spec(spec)
                .build();

        Exception ex = assertThrows(
                CommandLine.ParameterException.class,
                () -> resourceService.prepareResources(List.of(schemaResource), commandSpec));

        assertEquals(
                "Cannot open schema file not-exist.avsc. Schema path must be relative to the CLI.", ex.getMessage());
        assertNull(schemaResource.getSpec().get("schema"));
    }

    @Test
    void prepareSchemaResources_shouldEnrichSchemasWithUnionDependency() throws Exception {
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

        Map<String, SchemaData> filesMap = new HashMap<>(Map.of(
                "schemaUnion",
                        new SchemaData(
                                schemaUnion,
                                File.createTempFile("schemaUnion", ".avsc"),
                                List.of("com.example.one.Test1", "com.example.two.Test2", "com.example.three.Test3")),
                "schema1", new SchemaData(schema1, File.createTempFile("schema1", ".avsc"), List.of()),
                "schema2", new SchemaData(schema2, File.createTempFile("schema2", ".avsc"), List.of()),
                "schema3",
                        new SchemaData(
                                schema3, File.createTempFile("schema3", ".avsc"), List.of("com.example.one.Test1"))));

        Map<String, Resource> resourceMap = buildResources(filesMap);

        List<Resource> resources = new ArrayList<>(List.of(
                resourceMap.get("schema3"), resourceMap.get("schema2"),
                resourceMap.get("schema1"), resourceMap.get("schemaUnion")));

        List<Resource> sortedResources = resourceService.prepareResources(resources, commandSpec);

        for (Map.Entry<String, SchemaData> entry : filesMap.entrySet()) {
            entry.getValue().schemaTempFile.delete();
        }

        List<String> names =
                sortedResources.stream().map(r -> r.getMetadata().getName()).toList();

        assertTrue(names.indexOf("schema1") < names.indexOf("schema3"));
        for (var schemaName : List.of("schema1", "schema2", "schema3")) {
            assertTrue(names.indexOf("schemaUnion") > names.indexOf(schemaName));
        }
    }

    @Test
    void prepareSchemaResources_shouldSortResourcesByDependencies() throws Exception {
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

        Map<String, SchemaData> filesMap = new HashMap<>(Map.of(
                "schema1", new SchemaData(schema1, File.createTempFile("schema1", ".avsc"), List.of()),
                "schema2",
                        new SchemaData(
                                schema2, File.createTempFile("schema2", ".avsc"), List.of("com.example.one.Test1")),
                "schema3",
                        new SchemaData(
                                schema3, File.createTempFile("schema3", ".avsc"), List.of("com.example.two.Test2"))));

        Map<String, Resource> rMap = buildResources(filesMap);

        List<Resource> resources =
                new ArrayList<>(List.of(rMap.get("schema3"), rMap.get("schema2"), rMap.get("schema1")));

        var sortedResources = resourceService.prepareResources(resources, commandSpec);

        for (Map.Entry<String, SchemaData> entry : filesMap.entrySet()) {
            entry.getValue().schemaTempFile.delete();
        }

        assertEquals(
                List.of("schema1", "schema2", "schema3"),
                sortedResources.stream().map(r -> r.getMetadata().getName()).toList());
    }

    @Test
    void prepareSchemaResources_shouldHandleNestedFieldsWithDeepDependency() throws Exception {
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

        Map<String, SchemaData> filesMap = new HashMap<>(Map.of(
                "schemaLeaf", new SchemaData(schemaLeaf, File.createTempFile("schemaLeaf", ".avsc"), List.of()),
                "schemaNested",
                        new SchemaData(
                                schemaNested,
                                File.createTempFile("schemaNested", ".avsc"),
                                List.of("com.example.leaf.Leaf"))));

        Map<String, Resource> rMap = buildResources(filesMap);

        List<Resource> resources = new ArrayList<>(List.of(rMap.get("schemaNested"), rMap.get("schemaLeaf")));
        var sortedResources = resourceService.prepareResources(resources, commandSpec);

        for (Map.Entry<String, SchemaData> entry : filesMap.entrySet()) {
            entry.getValue().schemaTempFile.delete();
        }

        assertEquals(
                List.of("schemaLeaf", "schemaNested"),
                sortedResources.stream().map(r -> r.getMetadata().getName()).toList());
    }
}
