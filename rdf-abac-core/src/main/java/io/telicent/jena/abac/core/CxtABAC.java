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

import java.util.*;

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.DatasetGraph;

/**
 * Context for an ABAC execution.
 * A new context is created for each request.
 * There is a requestId - a unique key that can be used to identity the request.
 */
public class CxtABAC {

    public static void systemTrace(Track trace) { globalTrace = trace; }
    private static Track globalTrace = Track.NONE;

    // Unique request id.
    private final UUID id;
    /** The attributes values associated with the request. */
    private final AttributeValueSet requestAttributes;
    /** Accessor: Function to get attribute hierarchies */
    private final HierarchyGetter attrHierarchy;

    // --- Per request caches
    /** Cache of types of nodes for patterns "X where X has type T" */
    private final Cache<Node, Node> typeCache;

    /** Cache of evaluations within this request context. */
    private final Cache<String, ValueTerm> evalCache;

    /** Hierarchy lookup cache. Cache can't hold nulls. */
    private Cache<Attribute, Optional<Hierarchy>> hierarchyCache;

    /** The data being protected. */
    private final DatasetGraph baseData;
    /** Request environment values. These are not the attributes of the user.*/
    private final Map<Attribute, ValueTerm> environment;

    private Track trace;

    public static CxtABAC context(AttributeValueSet requestAttributes,
                                  HierarchyGetter attrHierarchy,
                                  DatasetGraph dsgBase) {
        Objects.requireNonNull(requestAttributes);
        Objects.requireNonNull(attrHierarchy);
        // No environment support yet.
        // No "security by rdf:type yet.
        return context(requestAttributes, attrHierarchy, null, null, dsgBase);
    }

    private static CxtABAC context(AttributeValueSet requestAttributes,
                                   HierarchyGetter attrHierarchy,
                                   Map<Attribute, ValueTerm> environment,
                                   Cache<Node, Node> typeCache,
                                   DatasetGraph dsgBase) {
        return new CxtABAC(requestAttributes, attrHierarchy, environment, typeCache, dsgBase);
    }

    private CxtABAC(AttributeValueSet requestAttributes, HierarchyGetter attrHierarchy,
                    Map<Attribute, ValueTerm> environment, Cache<Node, Node> typeCache,
                    DatasetGraph baseData) {
        this.requestAttributes = requestAttributes;
        this.attrHierarchy = attrHierarchy;

        this.typeCache = typeCache;
        this.evalCache = CacheFactory.createCache(ABAC.labelEvalCacheSize);
        // Having a label eval cache means the hierarchy cache is less important
        // because there are often (?) only a few distinct labels using the hierarchy
        // across the data.
        this.hierarchyCache = CacheFactory.createCache(ABAC.hierarchyCacheSize);

        this.baseData = baseData;
        this.environment = environment;
        this.trace = globalTrace;
        this.id = UUID.randomUUID();
    }

    public AttributeValueSet requestAttributes() { return requestAttributes; }

    public Hierarchy getHierarchy(Attribute attr) { return attrHierarchy.getHierarchy(attr); }

    public Collection<ValueTerm> getValue(Attribute attribute) {
        return requestAttributes.get(attribute);
    }

    //public Cache<Node, Node> typeCache()    { return typeCache; }

    public Cache<String, ValueTerm> labelEvalCache()  { return evalCache ; }

    public Cache<Attribute, Optional<Hierarchy>> hierarchyCache()  { return hierarchyCache; }

    public DatasetGraph data() { return baseData; }

    public Object requestId() { return id; }

    public void tracking(Track trace) { this.trace = trace; }
    public Track tracking() { return trace; }

    public boolean debug() { return tracking() == Track.DEBUG || tracking() == Track.TRACE; }
    public boolean trace() { return tracking() == Track.TRACE; }
}
