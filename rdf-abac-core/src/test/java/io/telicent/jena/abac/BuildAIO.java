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

import java.util.Objects;

import io.telicent.jena.abac.assembler.SecuredDatasetAssembler;
import io.telicent.jena.abac.core.Attributes;
import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.AuthzException;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.other.G;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.graph.GraphFactory;

/**
 * Ways to build authorization setups from a single file or dataset, of
 * data+labels+attributes graphs.
 * <p>
 * @see SecuredDatasetAssembler
 */
public class BuildAIO {

    public static DatasetGraphABAC setupByTriG(String aioFile, String dftTripleAttributes) {
//      public static void load(DatasetGraphAuthz dsg, String datafile) {
      Lang lang = RDFLanguages.filenameToLang(aioFile);
      if ( lang == null )
          throw new AuthzException("Can't determine the syntax: "+aioFile);
      if ( !RDFLanguages.isQuads(lang) )
          throw new AuthzException("Not a quads syntax: "+lang);
      DatasetGraph aio = DatasetGraphFactory.createTxnMem();
      RDFParser.source(aioFile).lang(lang).parse(aio);
      return setupByTriG(aio, dftTripleAttributes);
    }

    public static DatasetGraphABAC setupByTriG(DatasetGraph aio, String dftTripleAttributes) {
        DatasetGraph data = DatasetGraphFactory.createTxnMem();
        GraphUtil.addInto(data.getDefaultGraph(), aio.getDefaultGraph());
        // use copies to ensure complete isolation in tests.
        return buildFromGraphs(data,
                               copy(aio.getGraph(VocabAuthzTest.graphForLabels)),
                               copy(aio.getGraph(VocabAuthzTest.graphForRules)),
                               copy(aio.getGraph(VocabAuthzTest.graphForAttributes)));
    }

    private static Graph copy(Graph graph) {
        Graph safe = GraphFactory.createDefaultGraph();
        graph.getTransactionHandler().execute(()->GraphUtil.addInto(safe, graph));
        return safe;
    }

    /**
     * Build from data, graphs for labels and rules.
     * The labels and rules graphs are copied and not changed by updates.
     * Null for labels or rules is legal and means "none".
     */
    private static DatasetGraphABAC buildFromGraphs(DatasetGraph data, Graph labels, Graph rules, Graph attributesStoreGraph) {
        Objects.requireNonNull(data);
        LabelsStore labelsStore = null;

        String datasetDefaultLabel = null;

        if ( labels != null && ! labels.isEmpty() ) {
            labelsStore = Labels.createLabelsStore(labels);
            Node x = G.getZeroOrOneSP(labels, null, VocabAuthzTest.pDatasetDefaultLabel);
            if ( x != null ) {
                if ( ! Util.isSimpleString(x) )
                    Log.error(BuildAIO.class, "Ignored - not valid for authz:datasetDefaultLabel: "+x);
                else
                    datasetDefaultLabel = x.getLiteralLexicalForm();
            }
        }

        if ( rules != null && ! rules.isEmpty() )
            Log.error(BuildAIO.class, "Rules not supported");

        AttributesStore attributesStore = Attributes.buildStore(attributesStoreGraph);
        // No accessAttribute
        DatasetGraphABAC dsgz = ABAC.authzDataset(data, null, labelsStore, datasetDefaultLabel, attributesStore);
        return dsgz;
    }
}
