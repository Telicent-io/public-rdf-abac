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

package io.telicent.jena.abac.labels;

import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Transactional;

public interface LabelsStore { //implements Transactional {
    public List<String> labelsForTriples(Triple triple);

    public Transactional getTransactional();

    // Triples
    // Slots
    // Pattern

//    /**
//     * All labels that apply when no labels patterns match.
//     */
//    public List<String> defaultLabelsForTriples();

    /** A concrete or pattern triple */
    public default void add(Triple triple, String label) {
        add(triple, List.of(label));
    }

    /** A concrete or pattern triple */
    public void add(Triple triple, List<String> labels);

    /** A concrete or pattern triple */
    public default void add(Node subject, Node property, Node object, String label) {
        add(subject, property, object, List.of(label));
    }

    /** A concrete or pattern triple */
    public void add(Node subject, Node property, Node object, List<String> labels);

    /** Add a graph of label descriotions to the store. */
    public void add(Graph labelsData);

    /** Get labels as graph.
     *  This is more of a development and deployment helper; the graph may be very large.
     *  Returns a copy of the labels graph so it is not connected to the LabelsStore.
     */
    public Graph getGraph();

    public boolean isEmpty();
}
