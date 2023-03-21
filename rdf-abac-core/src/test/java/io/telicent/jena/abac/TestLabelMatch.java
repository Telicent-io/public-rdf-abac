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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Test;

public class TestLabelMatch {

    static LabelsStore labels = Labels.createLabelsStore();
    private static Node ANY_MARKER = Node.ANY;
    private static Node s = SSE.parseNode(":s");
    private static Node s1 = SSE.parseNode(":s1");
    private static Node p = SSE.parseNode(":p");
    private static Node p1 = SSE.parseNode(":p1");
    private static Node o = SSE.parseNode(":o");
    private static Node o1 = SSE.parseNode(":o1");
    static {
        labels.add(s, p, o, "spo");
        labels.add(s, p, ANY_MARKER, "sp_");
        labels.add(s, ANY_MARKER, ANY_MARKER, List.of("s__", "x__"));
        labels.add(ANY_MARKER, p, ANY_MARKER, "_p_");
        labels.add(ANY_MARKER, ANY_MARKER, ANY_MARKER, List.of("___", "???"));
    }

    static Triple triple(String string) { return SSE.parseTriple(string); }

    @Test public void label_match_basic() {
        LabelsStore emptyLabelStore = Labels.createLabelsStore();
        Triple t = triple("(:s1 :p1 :o1)");
        List<String> x = emptyLabelStore.labelsForTriples(t);
        assertEquals(List.of(), x);
    }

    @Test public void label_match_spo() {
        match(s, p, o, "spo");
    }

    @Test public void label_match_spx() {
        match(s, p, o1, "sp_");
    }

    @Test public void label_match_sxx() {
        match(s, p1, o1, "s__", "x__");
    }

    @Test public void label_match_xpx() {
        match(s1, p, o1, "_p_");
    }

    @Test public void label_match_xxx() {
        match(s1, p1, o1, "___", "???");
        match(s1, p1, o1, "???", "___");
    }

    private void match(Node s, Node p, Node o, String...expected) {
        Triple triple = Triple.create(s, p, o);
        List<String> x = labels.labelsForTriples(triple);
        List<String> e = Arrays.asList(expected);
        ABACTests.assertEqualsUnordered(e, x);
    }
}
