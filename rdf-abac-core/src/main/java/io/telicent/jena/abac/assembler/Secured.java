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

package io.telicent.jena.abac.assembler;

import static io.telicent.jena.abac.core.VocabAuthzDataset.*;
import static org.apache.jena.sparql.util.graph.GraphUtils.getAsStringValue;
import static org.apache.jena.sparql.util.graph.GraphUtils.getStringValue;

import io.telicent.jena.abac.core.*;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.util.graph.GraphUtils;

/** Helpers for working with assemblers of secured datasets */
public class Secured {

    /**
     * Build a DatasetGraphAuthz from configuration and a base dataset.
     * Return null if no configuration.
     */
    public static DatasetGraphABAC buildDatasetGraphAuthz(DatasetGraph base, Resource root) {
        String accessAttributes = getAccessAttributes(root);
        LabelsStore labels = labelsStore(root);
        String tripleDefaultLabel = getTripleDefaultLabel(root);
        if ( labels == null )
            // In-memory
            labels = Labels.createLabelsStore();

        AttributesStore attributesStore = attributesStore(root);
        DatasetGraphABAC dsgAuthz = new DatasetGraphABAC(base, accessAttributes, labels, tripleDefaultLabel, attributesStore);
        return dsgAuthz;
    }

    public static AttributesStore attributesStore(Resource root) {
        boolean localAttributeStore = GraphUtils.getAsRDFNode(root, pAttributes) != null;
        boolean remoteAttributeStore = GraphUtils.getAsRDFNode(root, pAttributesStoreURL) != null;

        if ( localAttributeStore && remoteAttributeStore )
            throw new AssemblerException(root, "User Attribute Store: Both remote and local local file.");

        if ( localAttributeStore )
            return localAttributesStore(root);
        if ( remoteAttributeStore )
            return remoteAttributesStore(root);
        throw new AssemblerException(root, "No attribute store specified.");
    }

    private static AttributesStore remoteAttributesStore(Resource root) {
        // The URL should contain the string "{user}" which is replaced when used with the user name from the request.
        // Required.
        String lookupUserTemplate = getAsStringValue(root, pAttributesStoreURL);
        if ( lookupUserTemplate == null )
            return null;
        lookupUserTemplate = environmentValue(root, lookupUserTemplate);

        // Same pattern except this is optional.
        String lookupHierarchyTemplate = getAsStringValue(root, pHierarchiesURL);
        lookupHierarchyTemplate = environmentValue(root, lookupHierarchyTemplate);

        return new AttributesStoreRemote(lookupUserTemplate, lookupHierarchyTemplate);
    }

    /**
     * Process a possible environment variable indirection.
     * Values in System properties are also tried.
     */
    private static String environmentValue(Resource root, String value) {
        if ( value == null )
            return null;
        if ( ! value.startsWith("env:") )
            return value;
        String envVar = value.substring("env:".length());
        String x = lookupEnvironmentVariable(envVar);
        if ( x == null )
            throw new AssemblerException(root, "Bad environment variable for remote user attribute store URL");
        return x;
    }

    private static String lookupEnvironmentVariable(String name) {
        String s1 = System.getenv().get(name);
        if ( s1 != null )
            return s1;
        String s2 = System.getProperty(name);
        return s2;
    }

    private static String getTripleDefaultLabel(Resource root) {
        String tripleDefaultAttributes = getStringValue(root, pTripleDefaultLabels);
        if ( tripleDefaultAttributes == null ) {
            // Java ...
            @SuppressWarnings("deprecation")
            String x = getStringValue(root, pTripleDefaultAttributes);
            tripleDefaultAttributes = x;
        }
        if ( tripleDefaultAttributes != null && tripleDefaultAttributes.isEmpty() )
            throw new AssemblerException(root, ":tripleDefaultLabels is an empty string (use \"!\" for 'deny all')");
        return tripleDefaultAttributes;
    }

    public static String getAccessAttributes(Resource root) {
        String accessAttributes = getStringValue(root, pAccessAttributes);
        if ( accessAttributes != null && accessAttributes.isEmpty() )
            throw new AssemblerException(root, ":accessAttributes is an empty string (use \"!\" for 'deny all')");
        return accessAttributes;
    }

    // ---- Labels

    public static LabelsStore labelsStore(Resource root) {
        RDFNode obj = GraphUtils.getAsRDFNode(root, pLabels);
        if ( obj == null )
            return null;
        if ( ! subjectInThisAssembler(obj) )
            // Treat as a file name.
            return labelsFile(root);
        return labelsStoreGraph(root, obj);
    }

    private static LabelsStore labelsFile(Resource root) {
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        try {
            String labelsRef = GraphUtils.getAsStringValue(root, pLabels);
            if ( labelsRef == null )
                return null;
            RDFDataMgr.read(dsg,labelsRef);
        } catch(Throwable th) {
            throw new AssemblerException(root, "Labels file reference must be an URI or filename string");
        }
        try {
            return Labels.createLabelsStore(dsg);
        } catch(Throwable th) {
            throw new AssemblerException(root, "Failed to parse the labels descriptions", th);
        }
    }

    private static LabelsStore labelsStoreGraph(Resource root, RDFNode obj) {
        Dataset ds = (Dataset)Assembler.general.open(obj.asResource());
        // Specific name?
        DatasetGraph dsg = ds.asDatasetGraph();
        return Labels.createLabelsStore(dsg);
    }

    private static boolean subjectInThisAssembler(RDFNode obj) {
        if ( ! obj.isResource() )
            return false;
        return obj.getModel().contains(obj.asResource(), null, (RDFNode)null);
    }

    // Misspelt.
    private static Property pBadAttributes = ResourceFactory.createProperty(VocabAuthzDataset.getURI()+"attribute");
    public static AttributesStore localAttributesStore(Resource root) {
        if ( null != GraphUtils.getAsRDFNode(root, pBadAttributes) ) {
            throw new AssemblerException(root, "Property \":attribute\" used (singular spelling) where \":attributes\" expected");
        }
        RDFNode obj = GraphUtils.getAsRDFNode(root, pAttributes);
        if ( obj == null )
            return null;
        return attributesStoreFile(root);
    }

    public static AttributesStore attributesStoreFile(Resource root) {
        String attributesStoreFilename;
        try {
            attributesStoreFilename = getAsStringValue(root, pAttributes);
            if ( attributesStoreFilename == null )
                return new AttributesStoreLocal();
        } catch(Throwable th) {
            throw new AssemblerException(root, "Attributes store file reference must be an URI or filename string");
        }
        try {
            return Attributes.readAttributesStore(attributesStoreFilename);
        } catch(Throwable th) {
            throw new AssemblerException(root, "Failed to parse the attributes store file '"+attributesStoreFilename+"'", th);
        }
    }
}
