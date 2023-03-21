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

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.core.TransactionalLock;
import org.apache.jena.sparql.graph.GraphZero;

/** A labels store with only one setting, fixed when created. */
public class LabelsStoreOne implements LabelsStore {

    private final List<String> labels;
    private final Transactional transactional = TransactionalLock.createMRPlusSW();

    /*package*/ LabelsStoreOne(String labels) {
        this.labels = (labels != null) ? List.of(labels) : List.of();
    }

    @Override
    public Transactional getTransactional() {
        return transactional;
    }

    @Override
    public List<String> labelsForTriples(Triple triple) {
        if ( ! triple.isConcrete() ) {
            Log.error(Labels.class, "Asked for labels for a triple with wildcards: "+NodeFmtLib.displayStr(triple));
            return null;
        }
        return labels;
    }

    @Override
    public void add(Triple triple, List<String> labels) {
        throw new UnsupportedOperationException("Can't add to LabelsStoreOne");
    }

    @Override
    public void add(Node subject, Node Property, Node object, List<String> labels) {
        throw new UnsupportedOperationException("Can't add to LabelsStoreOne");
    }

    @Override
    public void add(Graph labels) {
        throw new UnsupportedOperationException("Can't load into LabelsStoreOne");
    }

    @Override
    public boolean isEmpty() { return true; }

    @Override
    public Graph getGraph() { return GraphZero.instance(); }
}
