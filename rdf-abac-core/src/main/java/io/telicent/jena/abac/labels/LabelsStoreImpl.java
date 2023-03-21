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

import static org.apache.jena.sparql.util.NodeUtils.nullToAny;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.attributes.AttributeException;
import io.telicent.jena.abac.core.VocabAuthzLabels;
import io.telicent.jena.abac.labels.LabelsIndex.AuthzTriplePatternException;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.graph.*;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.other.G;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.system.Txn;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabelsStoreImpl implements LabelsStore {

    // The LabelsStoreImpl can be shared.
    // The LabelsIndex may change, including swapped to a different object.
    private static Logger LOG = LoggerFactory.getLogger(Labels.class);

    // MUTABLE - protected by the transactional.
    private final Graph labelsGraph;
    private PrefixMap pmap;

    private final AtomicReference<LabelsIndex> labelsIndex = new AtomicReference<>(null);
    private final Transactional transactional;

    /*package*/ static LabelsStore create(Graph labelsGraph, Transactional transactional) {
        // labelsGraph is the storage.
        LabelsStoreImpl store = new LabelsStoreImpl(labelsGraph, transactional);
        store.add(labelsGraph);
        return store;
    }

    private LabelsStoreImpl(Graph graph, Transactional transactional) {
        this.labelsGraph = Objects.requireNonNull(graph);
        this.transactional = Objects.requireNonNull(transactional);

        G.execTxn(graph, ()->{
            checkShape(graph);
            this.pmap = prefixMap(labelsGraph);
        });
        buildIndex();
    }

    /**
     * Return "tripleDefaultAttributes" or the system default setting if null.
     */
    private String attributeLabelOrSystemDefault(String tripleDefaultAttributes) {
        return tripleDefaultAttributes == null
                ? SysABAC.systemDefaultTripleAttributes
                : tripleDefaultAttributes;
    }

    @Override
    public void add(Graph labels) {
        Txn.executeWrite(transactional, ()-> {
            checkShape(labels);
            GraphUtil.addInto(labelsGraph, labels);
            buildIndex();
        });
    }

    @Override
    public Graph getGraph() {
        Graph gResult = GraphFactory.createDefaultGraph();
        Txn.executeRead(transactional,
                        ()->GraphUtil.addInto(gResult, labelsGraph)
                        );
        return gResult;
    }

    @Override
    public List<String> labelsForTriples(Triple triple) {
        if ( ! triple.isConcrete() ) {
            LOG.error("Asked for labels for a triple with wildcards: "+NodeFmtLib.displayStr(triple));
            return null;
        }

        try {
            ensureIndex();
        } catch (AuthzTriplePatternException ex) {
            LOG.error("Failed to update index: "+ex.getMessage());
            return List.of(SysABAC.denyLabel);
        }

        try {
            List<String> x = labelsIndex.get().match(triple);
            //FmtLog.info(ABAC.LOG, "%s : %s\n", str(triple), x);
            return x;
        } catch (Exception ex) {
            LOG.error("Failed to process: "+triple, ex);
            return null;
        }
    }

    private void ensureIndex() {
        try {
            if (labelsIndex.get() == null )
                buildIndex();
        } catch (RuntimeException ex) {
            LOG.warn("Failed to build index", ex);
            throw ex;
        }
    }

    private void buildIndex() {
        LabelsIndex index = LabelsIndex.buildIndex(labelsGraph);
        if ( index == null ) {}
        // Check zero or one default
        // Default settings.
        labelsIndex.set(index);
    }

    private static void buildIndex(Graph graph) {

    }

    @Override
    public boolean isEmpty() { return labelsGraph.isEmpty(); }

    @Override
    public Transactional getTransactional() { return transactional; }

    // [ authz:pattern "" ; authz:label "" ; authz:label ""]
    // one pattern, one or more labels.
    static void checkShape(Graph graph) {
        ExtendedIterator<Triple> iter = G.find(graph, Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        try {
            while(iter.hasNext() ) {
                Triple triple = iter.next();
                Node subject = triple.getSubject();
                boolean isOK = true;
                // Shape

                // Repeats the iterator - is this worth it?
                if ( ! G.hasOneSP(graph, subject, VocabAuthzLabels.pPattern) ) {
                    FmtLog.error(LOG, "Multiple patterns for same subject:: %s", NodeFmtLib.str(subject, prefixMap(graph)));
                    // XXX throw
                    continue;
                }
                // Pattern
                if ( Util.isSimpleString(triple.getObject()) ) {
                    // ERROR
                    // XXX throw
                    continue;
                }
                String patternStr = triple.getObject().getLiteralLexicalForm();

                if ( checkPatternString(patternStr) ) {
                    FmtLog.error(LOG, "Bad pattern: %s: pattern='%s'", NodeFmtLib.str(subject, prefixMap(graph)), patternStr);
                    // XXX throw
                    continue;
                }

                // Labels.
                List<Node> labels = G.listSP(graph, subject, VocabAuthzLabels.pLabel);
                if ( labels.isEmpty() ) {
                    FmtLog.error(LOG, "No labels for pattern: %s", NodeFmtLib.str(subject, prefixMap(graph)));
                    // XXX throw
                    continue;
                }

                if (! LabelsIndex.isPatternTriple(triple) ) {
                    // ERROR
                }

                labels.forEach(label-> {
                    if ( ! checkLabel(label) )
                        FmtLog.error(LOG, "Bad label: %sL : label=%s", NodeFmtLib.str(subject, prefixMap(graph)), label);
                } );

                // OK!
            }
        } finally { iter.close(); }
    }

    private static boolean checkPatternString(String patternStr) {
        throw new NotImplemented(LabelsStoreImpl.class.getSimpleName()+"::checkPatternString");
        //return false;
    }

    private static boolean checkLabel(Node labelNode) {
        if ( ! Util.isSimpleString(labelNode) )
            return false;
        return checkLabel(labelNode.getLiteralLexicalForm());
    }

    private static boolean checkLabel(String labelStr) {
        // Share parsing. with SecurityFilterByLabel.determineOutcome
        try {
            /*AttributeExpr aExpr =*/ AE.parseExpr(labelStr);
            return true;
        } catch (AttributeException ex) {
            return false;
        }
    }


    // Triple may be a pattern (wildcards), but must be S
    @Override
    public void add(Triple triple, List<String> labels) {
        add$(triple, labels);
    }

    /** Add a triple pattern but do not rebuild index. */
    private void add$(Triple triple, List<String> labels) {
        if ( !LabelsIndex.isPatternTriple(triple) )
            throw new AuthzTriplePatternException("Bad triple pattern: "+NodeFmtLib.str(triple));
        // XXX functions
        // Check labels.

        String s = tripleToString(triple);
        Node entry = NodeFactory.createBlankNode();
        Triple t1 = Triple.create(entry, VocabAuthzLabels.pPattern, NodeFactory.createLiteral(s));
        labelsGraph.add(t1);
        labels.forEach(x->{
            Node obj = NodeFactory.createLiteral(x);
            labelsGraph.add(entry, VocabAuthzLabels.pLabel, obj);
        });
        // No built index.
        labelsIndex.set(null);
    }

    /** Triple pattern to string. */
    private static String tripleToString(Triple triple) {
        String s = FmtUtils.stringForTriple(triple);
        return s;
    }

    /** String to (valid) triple pattern. */
    private TriplePattern stringToPattern(String string) {
        return LabelsIndex.parsePattern(string, pmap);
    }

    /** Add a triple pattern but do not rebuild index. */
    @Override
    public void add(Node subject, Node property, Node object, List<String> labels) {
        Node s = nullToAny(subject);
        Node p = nullToAny(property);
        Node o = nullToAny(object);
        Triple triple = Triple.create(s, p, o);
        add(triple, labels);
    }

    private static PrefixMap prefixMap(Graph graph) {
        return PrefixMapFactory.create(graph.getPrefixMapping()) ;
    }

    @Override
    public String toString() {
        ensureIndex();
        return RDFWriter.source(labelsGraph).lang(Lang.TTL).asString();
    }
}
