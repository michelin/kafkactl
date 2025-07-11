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

import com.michelin.kafkactl.model.Resource;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/** File service. */
@Singleton
public class FileService {
    /**
     * Get YAML files from given directory or file.
     *
     * @param fileOrDirectory The file/directory from which to search
     * @param recursive Search recursively or not
     * @return A list of files
     */
    public List<File> computeYamlFileList(File fileOrDirectory, boolean recursive) {
        return listAllFiles(new File[] {fileOrDirectory}, recursive).toList();
    }

    /**
     * Parse resource files to resources list.
     *
     * @param files A list of resource files
     * @return A list of resources
     */
    public List<Resource> parseResourceListFromFiles(List<File> files) {
        return files.stream()
                .map(File::toPath)
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .flatMap(this::parseResourceStreamFromString)
                .toList();
    }

    /**
     * Parse resources from strings to list.
     *
     * @param content The string to parse
     * @return A list of resources
     */
    public List<Resource> parseResourceListFromString(String content) {
        return parseResourceStreamFromString(content).toList();
    }

    /**
     * Parse resources from strings to stream.
     *
     * @param content The string to parse
     * @return A stream of resources
     */
    private Stream<Resource> parseResourceStreamFromString(String content) {
        Yaml yaml = new Yaml(new Constructor(Resource.class, new LoaderOptions()));
        return StreamSupport.stream(yaml.loadAll(content).spliterator(), false).map(Resource.class::cast);
    }

    /**
     * Get YAML files from given directory or file.
     *
     * @param rootDir The file/directory from which to search
     * @param recursive Search recursively or not
     * @return A list of files
     */
    private Stream<File> listAllFiles(File[] rootDir, boolean recursive) {
        return Arrays.stream(rootDir).flatMap(currentElement -> {
            if (currentElement.isDirectory()) {
                File[] files = currentElement.listFiles(file -> file.isFile()
                        && (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")));
                Stream<File> directories =
                        recursive ? listAllFiles(currentElement.listFiles(File::isDirectory), true) : Stream.empty();
                return Stream.concat(Stream.of(files), directories);
            } else {
                return Stream.of(currentElement);
            }
        });
    }
}
