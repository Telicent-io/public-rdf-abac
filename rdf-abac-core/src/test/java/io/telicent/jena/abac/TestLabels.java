/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.telicent.jena.abac;

import java.util.stream.Stream;

import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.riot.RIOT;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestLabels {
    static {
        JenaSystem.init();
        LogCtl.setLog4j2();
        RIOT.getContext().set(RIOT.symTurtleDirectiveStyle, "sparql");
    }

    private final static String DIR = "src/test/files/labels/";

    @ParameterizedTest(name = "{0}")
    @MethodSource("labels_files")
    public void labels(String filename, Integer expected) {
        test(filename, expected); }

    private void test(String filename, int count) {
        ABACTests.runTest(DIR+filename, count);
    }

    public static Stream<Arguments> labels_files() {
        return Stream.of(
                         Arguments.of("t01-1triple-yes.trig", 1),
                         Arguments.of("t02-1triple-no.trig", 0),

                         Arguments.of("t03-1triple-all.trig", 1),
                         Arguments.of("t04-1triple-deny.trig", 0),

                         Arguments.of("t05-1triple-dft.trig", 1),

                         Arguments.of("t07-1triple-any-yes.trig", 1),
                         Arguments.of("t08-1triple-any-no.trig", 0),
                         Arguments.of("t09-1triple-two-labels.trig", 0),

                         Arguments.of("t50-triples.trig", 2),
                         Arguments.of("t51-triples.trig", 2),

                         Arguments.of("t55-triples.trig", 2)
        );
    }

}
