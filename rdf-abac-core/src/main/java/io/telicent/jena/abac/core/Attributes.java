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

package io.telicent.jena.abac.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.util.NodeUtils;

public class Attributes {

    private static Shapes shapesAttributesStore = ABAC.readSHACL("attribute-store-shapes.shc");

    public static AttributesStore readAttributesStore(String filename) {
        Graph graph = RDFDataMgr.loadGraph(filename);
        if ( shapesAttributesStore != null ) {
            ValidationReport report = ShaclValidator.get().validate(shapesAttributesStore, graph);
            if ( ! report.conforms() ) {
                ShLib.printReport(System.err, report);
                throw new AuthzException("Bad attributes store file");
            }
        }
        AttributesStore attrStore = buildStore(graph);
        return attrStore;
    }

    private static String PREFIXES = """
            PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX sh:      <http://www.w3.org/ns/shacl#>
            PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>
            PREFIX authz:   <http://telicent.io/security#>
            """;

    private static String qUsers = PREFIXES+"\n"+"""
            SELECT ?user { [] authz:user ?user }
            """;
    private static String qUserAttributes = PREFIXES+"\n"+"""
            SELECT ?attribute { [] authz:user ?user ;
                                   authz:userAttribute ?attribute
                              }
            """;
    private static String qHierarchies = PREFIXES+"\n"+"""
            SELECT ?attribute ?values { [] authz:hierarchy [ authz:attribute ?attribute ; authz:attributeValues ?values ] }
            """;

    public static AttributesStore buildStore(Graph attributesStoreGraph) {
        AttributesStoreModifiable store = new AttributesStoreLocal();
        users(attributesStoreGraph, store);
        hierarchies(attributesStoreGraph, store);
        return store;
    }

    private static void users(Graph attributesStoreGraph, AttributesStoreModifiable store) {
        // Users
        RowSet rsUsers = QueryExec.graph(attributesStoreGraph).query(qUsers).select();
        Set<String> users = asStrings(rsUsers, "user");
        users.forEach(user->{
            Node u = NodeFactory.createLiteral(user);
            RowSet rsAttributes = QueryExec.graph(attributesStoreGraph).query(qUserAttributes).substitution("user", u).select();
            AttributeValueSet attributeSet = attributeValueSet(rsAttributes, "attribute");
            store.put(user, attributeSet);
        });
    }

    /**
     *  Build attribute-values for a row set from {@link #qUserAttributes}.
     *  <p>
     *  Attribute-values can be multiple in the "attribute" variable or split across multiple declarations.
     */
    private static AttributeValueSet attributeValueSet(RowSet rowSet, String varName) {
        List<AttributeValue> acc = new ArrayList<>();
        rowSet.forEachRemaining(row -> {
            Node node = row.get(varName);
            if ( ! Util.isSimpleString(node) )
                throw new AuthzException("Bad value for ?" + varName + ": " + NodeFmtLib.displayStr(node));
            String string = node.getLiteralLexicalForm();
            List<AttributeValue> attrValues = AE.parseAttrValueList(string);
            acc.addAll(attrValues);
        });
        return AttributeValueSet.of(acc);
    }


    private static Set<String> asStrings(RowSet rowSet, String varName) {
        return rowSet.stream().map(row -> {
            Node node = row.get(varName);
            if ( ! Util.isSimpleString(node) )
                throw new AuthzException("Bad value for ?" + varName + ": " + NodeFmtLib.displayStr(node));
            String value = node.getLiteralLexicalForm();
            return value;
        }).collect(Collectors.toSet());
    }

    private static void hierarchies(Graph attributesStoreGraph, AttributesStoreModifiable store) {
        RowSet hierarchies = QueryExec.graph(attributesStoreGraph).query(qHierarchies).select();
        hierarchies.forEachRemaining(row ->{
            String attrName = string(row.get("attribute"));
            String attrVals = string(row.get("values"));
            List<ValueTerm> values = AE.parseValueTermList(attrVals);
            Attribute a = new Attribute(attrName);
            Hierarchy h = new Hierarchy(a, values);
            store.addHierarchy(h);
        });
    }

    static String string(Node n) {
        if ( n == null )
            throw new NullPointerException("string for node");
        if ( ! n.isLiteral() )
            throw new AuthzException("Not a literal string");
        if ( ! NodeUtils.isSimpleString(n) )
            throw new AuthzException("Literal but not a plain string");
        return n.getLiteralLexicalForm();
    }
}
