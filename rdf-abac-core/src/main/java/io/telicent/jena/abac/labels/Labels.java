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

import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.QuadFilter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.sparql.core.*;

public class Labels {

    public static QuadFilter securityFilterByLabel(DatasetGraph dsgBase, LabelsGetter labels, String defaultLabel, CxtABAC cxt) {
        return new SecurityFilterByLabel(dsgBase, labels, defaultLabel, cxt);
    }

    private static final LabelsStore noLabelsStore = new LabelsStoreZero();

    public static LabelsStore emptyStore() {
        return noLabelsStore;
    }

    /** In-memory label store */
    public static LabelsStore createLabelsStore() {
        DatasetGraph tim = DatasetGraphFactory.createTxnMem();
        return createLabelsStore(tim);
    }

    /**
     * Create a label store, using the default graph of a dataset.
     * The labels graph is modified by {@link LabelsStore#add} operations.
     * <p>
     * Set {@code tripleDefaultAttributes} as "" for "no labels by default".
     *
     */
    public static LabelsStore createLabelsStore(DatasetGraph dsg) {
        return LabelsStoreImpl.create(dsg.getDefaultGraph(), dsg);
    }

    /**
     * Create a label store from a graph.
     * The labels graph is modified by {@link LabelsStore#add} operations.
     * The transactional is used to protect the graph during
     * <p>
     * Set {@code tripleDefaultAttributes} as "" for "no labels by default".
     *
     */
    public static LabelsStore createLabelsStore(Graph graph, Transactional transactional) {
        return LabelsStoreImpl.create(graph, transactional);
    }

    /**
     * Create a LabelsStore using graph as storage.
     * The labels graph may be modified by
     * {@link LabelsStore#add} operations.
     */
    public static LabelsStore createLabelsStore(Graph graph) {
        if ( graph instanceof GraphView ) {
            GraphView graphView = (GraphView)graph;
            return createLabelsStore(graph, graphView.getDataset());
        }
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        dsg.executeWrite(()->GraphUtil.addInto(dsg.getDefaultGraph(), graph));
        return createLabelsStore(graph, TransactionalLock.createMRSW());
    }

    /**
     * Fine grain control of filter logging.
     * This can be very verbose so sometimes only parts of test development need this.
     */
    public static void setLabelFilterLogging(boolean value) { SecurityFilterByLabel.setDebug(value); }

    public static boolean getLabelFilterLogging() {return  SecurityFilterByLabel.getDebug(); }
}
