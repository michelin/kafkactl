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
package com.michelin.kafkactl.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/** Test for Completion command. */
class CompletionTest {
    @Test
    void shouldGenerateCompletionScript() {
        Completion completion = new Completion();
        CommandLine cmd = new CommandLine(completion);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        int code = cmd.execute();
        assertEquals(0, code);
        String output = sw.toString();
        assertTrue(output.contains("_complete_kafkactl"));
        assertTrue(output.contains("complete -F _complete_kafkactl"));
        assertTrue(output.contains("kafkactl"));
    }

    @Test
    void shouldShowHelpMessage() {
        Completion completion = new Completion();
        CommandLine cmd = new CommandLine(completion);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        int code = cmd.execute("-h");
        assertEquals(0, code);
        String output = sw.toString();
        assertTrue(output.contains("Generate bash/zsh completion script for kafkactl"));
    }
}
