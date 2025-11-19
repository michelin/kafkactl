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

import static com.michelin.kafkactl.service.ResourceService.REFERENCES_FIELD;
import static com.michelin.kafkactl.service.ResourceService.SCHEMA_FIELD;
import static com.michelin.kafkactl.service.ResourceService.SCHEMA_FILE_FIELD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.michelin.kafkactl.model.Metadata;
import com.michelin.kafkactl.model.Resource;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@ExtendWith(MockitoExtension.class)
class ResourceServicePrepareResourcesTest {
    @Spy
    private FileService fileService;

    @Mock
    private CommandSpec commandSpec;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void shouldSetSchemaFromSchemaFilePath() {
        String schemaContent = normalize("""
                {
                   "namespace": "com.michelin.kafkactl",
                   "type": "record",
                   "name": "PersonAvro",
                   "fields": [
                     {
                       "name": "firstName",
                       "type": [
                         "string"
                       ],
                       "doc": "First name of the person"
                     },
                     {
                       "name": "lastName",
                       "type": [
                         "null",
                         "string"
                       ],
                       "default": null,
                       "doc": "Last name of the person"
                     }
                   ]
                 }""");

        Map<String, Object> spec = new HashMap<>();
        spec.put(SCHEMA_FILE_FIELD, new File("src/test/resources/person.avsc"));

        Resource schemaResource = Resource.builder()
                .kind("Schema")
                .metadata(Metadata.builder().name("test").build())
                .spec(spec)
                .build();

        resourceService.prepareResources(List.of(schemaResource), commandSpec);

        assertEquals(
                schemaContent, normalize(schemaResource.getSpec().get("schema").toString()));
    }

    @Test
    void shouldHandleFileNotFoundWhenPrepareResources() {
        Map<String, Object> spec = Map.of(SCHEMA_FILE_FIELD, "not-exist.avsc");

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
        Resource schemaCustomer = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("Customer").build())
                .spec(
                        Map.of(
                                SCHEMA_FIELD,
                                "{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"com.michelin.kafka.avro\", \"fields\": [{ \"name\": \"ref\", \"type\": \"string\" }]}"))
                .build();

        Resource schemaProduct = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("Product").build())
                .spec(
                        Map.of(
                                SCHEMA_FIELD,
                                "{\"type\":\"record\",\"name\":\"Product\",\"namespace\":\"com.michelin.kafka.avro\", \"fields\": [{ \"name\": \"ref\", \"type\": \"string\" }]}"))
                .build();

        Resource schemaOrder = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("Order").build())
                .spec(Map.of(
                        SCHEMA_FIELD,
                        normalize("""
                                {
                                  "type": "record",
                                  "name": "Order",
                                  "namespace": "com.michelin.kafka.avro",
                                  "fields": [
                                    { "name": "ref", "type": "com.michelin.kafka.avro.Customer"}
                                  ]
                                }
                                """),
                        REFERENCES_FIELD,
                        List.of(Map.of(
                                "name",
                                "com.michelin.kafka.avro.Customer",
                                "subject",
                                "abc.customer-value",
                                "version",
                                1))))
                .build();

        Resource schemaUnion = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("Union").build())
                .spec(Map.of(
                        SCHEMA_FIELD,
                        normalize("""
                            {
                             "type": "record",
                             "namespace": "com.michelin.kafka.avro",
                             "name": "AllTypes",
                             "fields": [
                               {
                                 "name": "oneOf",
                                 "type": [
                                   "com.michelin.kafka.avro.Customer",
                                   "com.michelin.kafka.avro.Product",
                                   "com.michelin.kafka.avro.Order"
                                 ]
                               }
                             ]
                            }
                            """),
                        REFERENCES_FIELD,
                        List.of(
                                Map.of(
                                        "name",
                                        "com.michelin.kafka.avro.Customer",
                                        "subject",
                                        "abc.customer-value",
                                        "version",
                                        1),
                                Map.of(
                                        "name",
                                        "com.michelin.kafka.avro.Product",
                                        "subject",
                                        "abc.product-value",
                                        "version",
                                        1),
                                Map.of(
                                        "name",
                                        "com.michelin.kafka.avro.Order",
                                        "subject",
                                        "abc.order-value",
                                        "version",
                                        1))))
                .build();

        List<Resource> sortedResources = resourceService.prepareResources(
                List.of(schemaUnion, schemaProduct, schemaOrder, schemaCustomer), commandSpec);

        List<String> sortedResourceNames =
                sortedResources.stream().map(r -> r.getMetadata().getName()).toList();

        assertTrue(sortedResourceNames.indexOf("Customer") < sortedResourceNames.indexOf("Order"));
        assertTrue(sortedResourceNames.indexOf("Product") < sortedResourceNames.indexOf("Order"));
        assertTrue(sortedResourceNames.indexOf("Order") < sortedResourceNames.indexOf("Union"));
    }

    @Test
    void shouldHandleNestedFieldsWithDeepDependencyWhenPrepareResources() {
        Resource schemaLeaf = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("schemaLeaf").build())
                .spec(Map.of(SCHEMA_FIELD, normalize("""
                                {
                                  "type": "record",
                                  "name": "Leaf",
                                  "namespace": "com.example.leaf",
                                    "fields": [
                                        {
                                        "name": "leafField",
                                        "type": "string"
                                        }
                                    ]
                                }""")))
                .build();

        Resource schemaNested = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("schemaNested").build())
                .spec(Map.of(
                        SCHEMA_FIELD,
                        normalize("""
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
                                }"""),
                        REFERENCES_FIELD,
                        List.of(Map.of("name", "com.example.leaf.Leaf", "subject", "leaf-subject", "version", 1))))
                .build();

        List<Resource> sortedResources =
                resourceService.prepareResources(List.of(schemaNested, schemaLeaf), commandSpec);

        assertEquals(
                List.of("schemaLeaf", "schemaNested"),
                sortedResources.stream().map(r -> r.getMetadata().getName()).toList());
    }

    @Test
    void shouldHandleMissingNameInReferencesGracefully() {
        Resource schemaCustomer = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("Customer").build())
                .spec(
                        Map.of(
                                SCHEMA_FIELD,
                                "{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"com.michelin.kafka.avro\", \"fields\": [{ \"name\": \"ref\", \"type\": \"string\" }]}"))
                .build();

        Resource schemaOrder = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("Order").build())
                .spec(Map.of(
                        SCHEMA_FIELD,
                        normalize("""
                                {
                                  "type": "record",
                                  "name": "Order",
                                  "namespace": "com.michelin.kafka.avro",
                                  "fields": [
                                    { "name": "ref", "type": "com.michelin.kafka.avro.Customer"}
                                  ]
                                }
                                """),
                        REFERENCES_FIELD,
                        List.of(Map.of("subject", "abc.customer-value", "version", 1))))
                .build();

        when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));

        Exception ex = assertThrows(
                CommandLine.ParameterException.class,
                () -> resourceService.prepareResources(List.of(schemaOrder, schemaCustomer), commandSpec));

        assertEquals("Schema reference is missing required fields \"name\".", ex.getMessage());
    }

    @Test
    void shouldHandleMissingSubjectInReferencesGracefully() {
        Resource schemaCustomer = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("Customer").build())
                .spec(
                        Map.of(
                                SCHEMA_FIELD,
                                "{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"com.michelin.kafka.avro\", \"fields\": [{ \"name\": \"ref\", \"type\": \"string\" }]}"))
                .build();

        Resource schemaOrder = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("Order").build())
                .spec(Map.of(
                        SCHEMA_FIELD,
                        normalize("""
                                {
                                  "type": "record",
                                  "name": "Order",
                                  "namespace": "com.michelin.kafka.avro",
                                  "fields": [
                                    { "name": "ref", "type": "com.michelin.kafka.avro.Customer"}
                                  ]
                                }
                                """),
                        REFERENCES_FIELD,
                        List.of(Map.of("name", "com.michelin.kafka.avro.Customer", "version", 1))))
                .build();

        when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));

        Exception ex = assertThrows(
                CommandLine.ParameterException.class,
                () -> resourceService.prepareResources(List.of(schemaOrder, schemaCustomer), commandSpec));

        assertEquals("Schema reference is missing required fields \"subject\".", ex.getMessage());
    }

    @Test
    void shouldHandleMissingVersionInReferencesGracefully() {
        Resource schemaCustomer = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("Customer").build())
                .spec(
                        Map.of(
                                SCHEMA_FIELD,
                                "{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"com.michelin.kafka.avro\", \"fields\": [{ \"name\": \"ref\", \"type\": \"string\" }]}"))
                .build();

        Resource schemaOrder = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("Order").build())
                .spec(Map.of(
                        SCHEMA_FIELD,
                        normalize("""
                                {
                                  "type": "record",
                                  "name": "Order",
                                  "namespace": "com.michelin.kafka.avro",
                                  "fields": [
                                    { "name": "ref", "type": "com.michelin.kafka.avro.Customer"}
                                  ]
                                }
                                """),
                        REFERENCES_FIELD,
                        List.of(Map.of("name", "com.michelin.kafka.avro.Customer", "subject", "abc.customer-value"))))
                .build();

        when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));

        Exception ex = assertThrows(
                CommandLine.ParameterException.class,
                () -> resourceService.prepareResources(List.of(schemaOrder, schemaCustomer), commandSpec));

        assertEquals("Schema reference is missing required fields \"version\".", ex.getMessage());
    }

    @Test
    void shouldSortSchemaReferencesWithSimpleDependencies() {
        Map<String, List<SchemaReference>> deps = Map.of(
                "com.michelin.kafka.avro.A", List.of(),
                "com.michelin.kafka.avro.B", List.of(),
                "com.michelin.kafka.avro.C", List.of(new SchemaReference("com.michelin.kafka.avro.B", "subject", 1)),
                "com.michelin.kafka.avro.D",
                        List.of(
                                new SchemaReference("com.michelin.kafka.avro.A", "subject", 1),
                                new SchemaReference("com.michelin.kafka.avro.C", "subject", 1)));

        List<String> sorted = resourceService.sortSchemaReferences(deps);

        assertTrue(sorted.indexOf("com.michelin.kafka.avro.A") < sorted.indexOf("com.michelin.kafka.avro.D"));
        assertTrue(sorted.indexOf("com.michelin.kafka.avro.C") < sorted.indexOf("com.michelin.kafka.avro.D"));
        assertTrue(sorted.indexOf("com.michelin.kafka.avro.B") < sorted.indexOf("com.michelin.kafka.avro.C"));
    }

    @Test
    void shouldSortSchemaReferencesWithMultipleDependencies() {
        Map<String, List<SchemaReference>> deps = Map.of(
                "com.michelin.kafka.avro.Base1", List.of(),
                "com.michelin.kafka.avro.Base2", List.of(),
                "com.michelin.kafka.avro.Base3", List.of(),
                "com.michelin.kafka.avro.Composite",
                        List.of(
                                new SchemaReference("com.michelin.kafka.avro.Base1", "subject", 1),
                                new SchemaReference("com.michelin.kafka.avro.Base2", "subject", 1),
                                new SchemaReference("com.michelin.kafka.avro.Base3", "subject", 1)));

        List<String> sorted = resourceService.sortSchemaReferences(deps);

        assertTrue(
                sorted.indexOf("com.michelin.kafka.avro.Base1") < sorted.indexOf("com.michelin.kafka.avro.Composite"));
        assertTrue(
                sorted.indexOf("com.michelin.kafka.avro.Base2") < sorted.indexOf("com.michelin.kafka.avro.Composite"));
        assertTrue(
                sorted.indexOf("com.michelin.kafka.avro.Base3") < sorted.indexOf("com.michelin.kafka.avro.Composite"));
    }

    @Test
    void shouldHandleEmptyDependencies() {
        Map<String, List<SchemaReference>> deps = Map.of(
                "com.michelin.kafka.avro.Standalone1", List.of(),
                "com.michelin.kafka.avro.Standalone2", List.of(),
                "com.michelin.kafka.avro.Standalone3", List.of());

        List<String> sorted = resourceService.sortSchemaReferences(deps);

        assertEquals(3, sorted.size());
        assertTrue(sorted.containsAll(deps.keySet()));
    }

    @Test
    void shouldDetectCyclicDependency() {
        Map<String, List<SchemaReference>> deps = new HashMap<>();
        deps.put("com.example.A", List.of(new SchemaReference("com.example.B", "subject", 1)));
        deps.put("com.example.B", List.of(new SchemaReference("com.example.A", "subject", 1)));

        assertThrows(IllegalStateException.class, () -> resourceService.sortSchemaReferences(deps));
    }

    @Test
    void shouldDetectSelfReference() {
        Map<String, List<SchemaReference>> deps =
                Map.of("com.example.A", List.of(new SchemaReference("com.example.A", "subject", 1)));

        assertThrows(IllegalStateException.class, () -> resourceService.sortSchemaReferences(deps));
    }

    @Test
    void shouldHandleDiamondDependency() {
        Map<String, List<SchemaReference>> deps = Map.of(
                "com.example.Base", List.of(),
                "com.example.Left", List.of(new SchemaReference("com.example.Base", "subject", 1)),
                "com.example.Right", List.of(new SchemaReference("com.example.Base", "subject", 1)),
                "com.example.Top",
                        List.of(
                                new SchemaReference("com.example.Left", "subject", 1),
                                new SchemaReference("com.example.Right", "subject", 1)));

        List<String> sorted = resourceService.sortSchemaReferences(deps);

        assertTrue(sorted.indexOf("com.example.Base") < sorted.indexOf("com.example.Left"));
        assertTrue(sorted.indexOf("com.example.Base") < sorted.indexOf("com.example.Right"));
        assertTrue(sorted.indexOf("com.example.Left") < sorted.indexOf("com.example.Top"));
        assertTrue(sorted.indexOf("com.example.Right") < sorted.indexOf("com.example.Top"));
    }

    @Test
    void shouldAddNamespacesBeforeSchemasWhenPrepareResource() {
        Resource schema1 = Resource.builder()
                .kind("Schema")
                .apiVersion("v1")
                .metadata(Metadata.builder().name("schema1").build())
                .spec(
                        new HashMap<>(
                                Map.of(
                                        SCHEMA_FIELD,
                                        "{\"type\":\"record\",\"name\":\"Test1\",\"namespace\":\"com.example\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}]}")))
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
                .spec(
                        new HashMap<>(
                                Map.of(
                                        SCHEMA_FIELD,
                                        "{\"type\":\"record\",\"name\":\"Test2\",\"namespace\":\"com.example\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}]}")))
                .build();

        List<Resource> input = List.of(schema1, namespace, schema2);

        List<Resource> result = resourceService.prepareResources(input, commandSpec);

        assertEquals("Namespace", result.get(0).getKind());
        assertEquals("Schema", result.get(1).getKind());
        assertEquals("Schema", result.get(2).getKind());
    }

    @Test
    void shouldOrderResourcesFromYamlFileWithNamespacesACLsRoleBindingsFirst() {
        File yamlFile = new File("src/test/resources/namespaces/unordered-resources.yml");
        List<Resource> resources = resourceService.parseResources(Optional.of(yamlFile), false, commandSpec);
        List<Resource> sorted = resourceService.prepareResources(resources, commandSpec);

        List<String> expectedOrder = List.of(
                "demo",
                "myRoleBinding1",
                "myRoleBinding2",
                "acl-group",
                "acl-topic",
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
        File yamlFile = new File("src/test/resources/namespaces/ordered-resources.yml");
        List<Resource> resources = resourceService.parseResources(Optional.of(yamlFile), false, commandSpec);

        List<Resource> sorted = resourceService.prepareResources(resources, commandSpec);

        List<String> expectedOrder =
                List.of("demo", "myRoleBinding1", "acl-topic", "demoPrefix.topic_64-demo.Car", "demoPrefix.topic_64");
        List<String> actualOrder =
                sorted.stream().map(r -> r.getMetadata().getName()).toList();

        assertEquals(expectedOrder, actualOrder);
    }

    private String normalize(String str) {
        return str.replace("\n", "").replace("\r", "").replace("\t", "").replace(" ", "");
    }

    @Test
    void shouldApplySchemasWithSameSchemaFile() {
        Map<String, Object> spec = new HashMap<>();
        spec.put(SCHEMA_FILE_FIELD, new File("src/test/resources/person.avsc"));

        Resource schemaResource1 = Resource.builder()
                .kind("Schema")
                .metadata(Metadata.builder().name("test1").build())
                .spec(spec)
                .build();

        Resource schemaResource2 = Resource.builder()
                .kind("Schema")
                .metadata(Metadata.builder().name("test2").build())
                .spec(spec)
                .build();

        var actual = resourceService.prepareResources(List.of(schemaResource1, schemaResource2), commandSpec);

        assertEquals(2, actual.size());
        assertEquals("test1", actual.getFirst().getMetadata().getName());
        assertEquals("test2", actual.get(1).getMetadata().getName());
    }
}
