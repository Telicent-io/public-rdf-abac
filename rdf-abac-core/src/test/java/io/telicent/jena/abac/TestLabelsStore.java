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

import static io.telicent.jena.abac.ABACTests.assertEqualsUnordered;
import static org.apache.jena.sparql.sse.SSE.parseTriple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsIndex;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test storing labels.
 *
 * See {@link TestLabelMatch} for matching labels more generally.
 */
public class TestLabelsStore {

    private static Triple triple1 = parseTriple("(:s :p 123)");
    private static Triple triple2 = parseTriple("(:s :p 'xyz')");

    @Test public void labelsStore_1() {
        LabelsStore store = Labels.createLabelsStore();
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of(), x);
    }

    @Test public void labelsStore_2() {
        LabelsStore store = Labels.createLabelsStore();
        store.add(triple1, "label");
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of("label"), x);
    }

    @Test public void labelsStore_3() {
        LabelsStore store = Labels.createLabelsStore();
        store.add(triple1, "label1");
        store.add(triple2, "labelx");
        store.add(triple1, "label2");
        List<String> x = store.labelsForTriples(triple1);
        assertEqualsUnordered(List.of("label1", "label2"), x);
    }

    @Test public void labelsStore_4() {
        LabelsStore store = Labels.createLabelsStore();
        store.add(triple1, "label1");
        store.add(triple2, "label2");
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of("label1"), x);
    }

    private static String labelsGraph = """
            PREFIX foo: <http://example/>
            PREFIX authz: <http://telicent.io/security#>
            ## No bar:
            [ authz:pattern 'bar:s bar:p1 123' ;  authz:label "allowed" ] .

            """;
    private static Graph BAD_PATTERN = RDFParser.fromString(labelsGraph).lang(Lang.TTL).toGraph();

    @Test public void labels_bad_labels_graph() {
        assertThrows(LabelsIndex.classOfInvalidPatternException(),
                     ()-> ABACTests.loggerAtLevel(LabelsIndex.class, "FATAL",
                                                  ()->Labels.createLabelsStore(BAD_PATTERN)));
    }

    @Disabled
    @Test public void labels_add_bad_label() {
        LabelsStore store = Labels.createLabelsStore();
        assertThrows(LabelsIndex.classOfInvalidPatternException(),
                     ()->{
                         store.add(triple1, "not_good");
                         store.labelsForTriples(triple1);
                     });
    }

    @Disabled
    @Test public void labels_add_bad_labels_graph() {
        LabelsStore store = Labels.createLabelsStore();
        String gs = """
                PREFIX : <http://example>
                PREFIX authz: <http://telicent.io/security#>
                [ authz:pattern 'jibberish' ;  authz:label "allowed" ] .
                """;
        Graph addition = RDFParser.fromString(gs).lang(Lang.TTL).toGraph();

        assertThrows(LabelsIndex.classOfInvalidPatternException(),
                     ()-> {
                         ABACTests.loggerAtLevel(LabelsIndex.class, "FATAL", ()->store.add(addition));
                     });
    }

}
