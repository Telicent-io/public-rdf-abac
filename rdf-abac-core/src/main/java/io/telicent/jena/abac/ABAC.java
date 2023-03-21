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

import java.io.InputStream;
import java.util.Set;

import io.telicent.jena.abac.assembler.SecuredDatasetAssembler;
import io.telicent.jena.abac.core.*;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsGetter;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreZero;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import org.apache.jena.sparql.graph.GraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Programmatic API to the Attribute-Based Access Control functionality.
 *
 * @see SecuredDatasetAssembler
 */
public class ABAC {

    public static Logger AzLOG = LoggerFactory.getLogger("io.telicent.jena.abac.Authz");
    public static Logger AttrLOG = LoggerFactory.getLogger("io.telicent.jena.abac.Attribute");

    /**
     * Operate with old style attributes only.
     * This applies to labels, attribute store description, and remote attribute store.
     * No value, no quotes, any character set - no white space or =
     */
    public static boolean LEGACY = true;

    /** Per request label evaluation cache size. */
    public static final int labelEvalCacheSize = 10 ;

    /**
     * Per request hierarchy retrieval cache size.
     * This could become a global cache. The answers are not request sensitive.
     */
    public static int hierarchyCacheSize = 10 ;

    /**
     * Create a {@link DatasetGraphABAC}. One of arguments {@code rules} or
     * {@code labels}, but not both, may be null.
     */
    public static DatasetGraphABAC authzDataset(DatasetGraph dsgBase, LabelsStore labels, String datasetDefaultLabel, AttributesStore attributesStore) {
        return authzDataset(dsgBase, null, labels, datasetDefaultLabel, attributesStore);
    }

    /**
     * Create a {@link DatasetGraphABAC}. One of arguments {@code rules} or
     * {@code labels}, but not both, may be null.
     */
    public static DatasetGraphABAC authzDataset(DatasetGraph dsgBase, String accessAttributes,
                                                 LabelsStore labels, String datasetDefaultLabel,
                                                 AttributesStore attributesStore) {
        return new DatasetGraphABAC(dsgBase, accessAttributes, labels, datasetDefaultLabel, attributesStore);
    }

    /**
     * Build an attribute-enforcing DatasetGraph. For programmatic/API use,
     * and in unit tests.
     * <p>
     * This is not used by Fuseki.
     */
    public static DatasetGraph requestDataset(DatasetGraphABAC dsgAuthz, AttributeValueSet attributes, HierarchyGetter function) {
        CxtABAC cxt = CxtABAC.context(attributes, function, dsgAuthz.getBase());
        DatasetGraph dsg = filterDataset(dsgAuthz, cxt);
        return dsg;
    }

    /**
     * Build an attribute-enforcing DatasetGraph. For programmatic/API use,
     * and in unit tests.
     * <p>
     * This is not used by Fuseki.
     */
    public static DatasetGraph requestDataset(DatasetGraphABAC dsgAuthz, AttributeValueSet attributes, AttributesStore attrStore) {
        CxtABAC cxt = CxtABAC.context(attributes, attrStore, dsgAuthz.getBase());
        DatasetGraph dsg = filterDataset(dsgAuthz, cxt);
        return dsg;
    }

    /**
     * Create an attribute-filtered dataset for a context.
     *
     * @see #requestDataset
     */
    public static DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt) {
        return filterDataset(dsgAuthz.getBase(), dsgAuthz.labelsStore(), dsgAuthz.getDefaultLabel(), cxt);
    }

    /**
     * Create an attribute-filtered dataset for a context.
     * <p>
     * "No label store" (null) is not the same as an empty label store
     * ({@link LabelsStoreZero}). "No store" mean the label filter isn't even
     * incorporated into decisions whereas an empty store may have a default.
     * <p>The DatasetGraph is the data storage dataset.
     */
    public static DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, String defaultLabel, CxtABAC cxt) {
        QuadFilter filter = null;
        if ( labels != null ) {
            LabelsGetter getter = triple->labels.labelsForTriples(triple);
            filter = combineFilter(filter, Labels.securityFilterByLabel(dsgBase, getter, defaultLabel, cxt));
        }
        DatasetGraph dsg = new DatasetGraphFilteredView(dsgBase, filter, Set.of());
        return dsg;
    }

    private static QuadFilter combineFilter(QuadFilter filter1, QuadFilter filter2) {
        if ( filter1 == null )
            return filter2;
        if ( filter2 == null )
            return filter1;
        return q -> filter1.test(q) && filter2.test(q);
    }


    /** Read SHACL from a classpath resource. */
    public static Shapes readSHACL(String resource) {
        InputStream in  = ABAC.class.getClassLoader().getResourceAsStream(resource);
        if ( in == null )
            return null;
        Graph gShacl = GraphFactory.createDefaultGraph();
        RDFParser.source(in).lang(Lang.SHACLC).parse(gShacl);
        Shapes shapes = ShaclValidator.get().parse(gShacl);
        return shapes;
    }
}
