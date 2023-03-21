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

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.telicent.jena.abac.core.AuthzException;
import io.telicent.jena.abac.core.VocabAuthzLabels;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.other.G;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.TokenType;
import org.apache.jena.riot.tokens.Tokenizer;
import org.apache.jena.riot.tokens.TokenizerText;
import org.apache.jena.sparql.core.Match;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * Index of triples, to access their labels.
 *
 * Pure RDF encoding.<br/>
 * Prefixes are taken from the labels graph.
 * <pre>
 * [ authz:pattern "ANY :p :o" ; authz:label "attr1 | attr2" ]
 * </pre>
 * "_" is a synonym for "ANY".
 */
public class LabelsIndex {
    // Maybe (also) pattern as triples: [ authz:subject [] ; authz:predicate :p ; authz:object 123 ; authz:label "attr1 | attr2" ]

    // Only kept for "information"
    private final Graph labels;

    // Indexing for triples (by pattern) to label.
    // The "triple" in the value slot of these maps is a pattern.
    //
    // Find order:
    // S P O
    // S P ANY
    // S ANY ANY
    // ANY P ANY
    // ANY ANY ANY

    // Exact match: structure is S->(?->pattern)
    private Map<Node, Map<Node, TriplePattern>> exact = new HashMap<>();

    // Simple structures. These are the various supported patterns for label matches.
    private Map<Node, TriplePattern> SP = new HashMap<>();
    private Map<Node, TriplePattern> S  = new HashMap<>();
    private Map<Node, TriplePattern> P  = new HashMap<>();
    private Map<Node, TriplePattern> ANY = new HashMap<>();

    public LabelsIndex(Graph labels) {
        this.labels = labels;
    }

    private Cache<Triple, List<String>> cache = CacheFactory.createOneSlotCache();

    /**
     * Match in order:
     * <ul>
     * <li>S P O
     * <li>S P <em>any</em>
     * <li>S <em>any</em> <em>any</em>
     * <li><em>any</em> P <em>any</em>
     * </ul>
     * The pattern "any any any" is the effect default for when no other match occurs.
     * @return List of labels.
     */
    public List<String> match(Triple triple) {
        List<String> acc;

        Map<Node, TriplePattern> subMap = exact.computeIfAbsent(triple.getSubject(), sx->new HashMap<>());
        if ( subMap != null ) {
            acc = labelsForTriple(triple,subMap);
            if ( ! acc.isEmpty() )
                return acc;
        }
        // Patterns.
        acc = labelsForTriple(triple, SP);
        if ( ! acc.isEmpty() )
            return acc;
        acc = labelsForTriple(triple, S);
        if ( ! acc.isEmpty() )
            return acc;
        acc = labelsForTriple(triple, P);
        if ( ! acc.isEmpty() )
            return acc;
        acc = labelsForTriple(triple, ANY);
        if ( ! acc.isEmpty() )
            return acc;
        return List.of();
    }

    private List<String> labelsForTriple(Triple triple, Map<Node, TriplePattern> sector) {
        if ( sector.isEmpty() )
            return List.of();
        List<String> acc = new ArrayList<>();
        List<Node> xs = patternMatches(triple, sector);
        if ( ! xs.isEmpty() ) {
            for ( Node x  : xs ) {
                List<Node> attrLabels = G.listSP(labels, x, VocabAuthzLabels.pLabel);
                attrLabels.forEach(a->acc.add(a.getLiteralLexicalForm()));
            }
        }
        return acc;
    }

    private static List<Node> patternMatches(Triple triple, Map<Node, TriplePattern> sector) {
        return sector.entrySet().stream().filter(e->{
            Node k = e.getKey();
            TriplePattern m = e.getValue();
            return Match.match(triple, m.subject(), m.predicate(), m.object());
        }).map(Entry::getKey).collect(Collectors.toList());
    }

    // ---- Index builder
    /**
     * Build the index.
     * Returns new index or throws an exception.
     * The graph of encoded labels should have been checked for shape before this function is called.
     */
    public static LabelsIndex buildIndex(Graph labels) {
        return G.calcTxn(labels, ()->buildIndex$(labels));
    }

    private static LabelsIndex buildIndex$(Graph labels) {
        LabelsIndex labelsIndex = new LabelsIndex(labels);
        PrefixMap pmap = PrefixMapFactory.create(labels.getPrefixMapping()) ;
        // [ authz:pattern "" ; authz:label "" ; authz:label ""]
        //    Possibly several authz:label "" per pattern.
        ExtendedIterator<Triple> patterns = G.find(labels, null, VocabAuthzLabels.pPattern, null);
        try {
            while(patterns.hasNext()) {

                Triple t = patterns.next();
                Node x = t.getSubject();
                Node pattern = t.getObject();

                TriplePattern m = null;
                try {
                    // Can throw AuthzTriplePatternException
                    // Checks should have been done.
                    m = parsePattern(pattern, pmap);
                } catch (AuthzTriplePatternException ex) {
                    // Message was logged.
                    throw ex;
                }
                List<Node> attrLabels = G.listSP(labels, x, VocabAuthzLabels.pLabel);

                // ----
                if ( attrLabels.isEmpty() ) {}
                if ( m == null )
                    continue;
                // ----
                labelsIndex.insertIntoIndex(x, m, attrLabels);
            }
        } finally { patterns.close(); }
        return labelsIndex;
    }

    /** Check whetheer a triple pattern is indexable. */
    /*package*/ static boolean isPatternTriple(Triple triple) {
        // SPO, SP, S or P, or ANY
        Objects.requireNonNull(triple);

        if ( triple.getSubject().isConcrete() )
            // SPO, SP, S
            return true;
        if ( triple.getPredicate().isConcrete() )
            // S
            return true;
        if ( triple.equals(Triple.ANY) )
            return true;
        return false;
    }

    /** Add an entry into the index. */
    /*package*/ void insertIntoIndex(Node x, TriplePattern m, List<Node> attrLabels) {
        for(Node label : attrLabels) {
            if ( ! Util.isSimpleString(label) ) {
                Log.warn(LabelsIndex.class, "Not a string literal: "+label );
                continue;
            }
            Node s = m.subject();
            Node p = m.predicate();
            Node o = m.object();
            if ( s.isConcrete() && p.isConcrete() && o.isConcrete() ) {
                Map<Node, TriplePattern> subMap = exact.computeIfAbsent(s, sx->new HashMap<>());
                subMap.put(x, m);
            } else if ( s.isConcrete() && p.isConcrete() && ! o.isConcrete() ) {
                SP.put(x, m);
            } else if ( s.isConcrete() && ! p.isConcrete() && ! o.isConcrete() ) {
                S.put(x, m);
            } else if ( ! s.isConcrete() && p.isConcrete() && ! o.isConcrete() ) {
                P.put(x, m);
            } else if ( ! s.isConcrete() && ! p.isConcrete() && ! o.isConcrete() ) {
                ANY.put(x, m);
            } else {
                Log.warn(LabelsIndex.class, "Pattern not supported: "+m);
            }
        }
    }

    private static TriplePattern parsePattern(Node pattern, PrefixMap pmap) {
        if ( ! pattern.isLiteral() ) {
            Log.error(LabelsIndex.class, "Not a literal: "+pattern);
            return null;
        }
        return parsePattern(pattern.getLiteralLexicalForm(), pmap);
    }

    /*package*/ static TriplePattern parsePattern(String pattern, PrefixMap pmap) {
        try {
            // RIOT tokenizer.
            Tokenizer tok = TokenizerText.fromString(pattern);
            Node s = tokenToNode(tok.next(), pmap);
            Node p = tokenToNode(tok.next(), pmap);
            Node o = tokenToNode(tok.next(), pmap);
//            if ( tok.hasNext() )
//                throw new RiotException("Extra tokens after pattern");
            return TriplePattern.create(s,p,o);
        }
        catch (RiotException ex) {
            String msg =  "Bad pattern: \""+pattern+"\": "+ex.getMessage();
            Log.error(LabelsIndex.class, msg);
            throw new AuthzTriplePatternException(msg);
            //return null;
        }
    }

    /*package*/ static class AuthzTriplePatternException extends AuthzException {
        public AuthzTriplePatternException(String msg) { super(msg); }
    }

    /*
     * For tests ...
     */
    public static Class<? extends AuthzException> classOfInvalidPatternException() { return AuthzTriplePatternException.class; }

    // Token to node.
    private static Node tokenToNode(Token t, PrefixMap pmap) {
        if ( t.getType() == TokenType.UNDERSCORE )
            return Node.ANY;
        if ( t.getType() == TokenType.KEYWORD && t.getImage().equalsIgnoreCase("ANY") )
            return Node.ANY;
        Node n = t.asNode(pmap);
        if ( n.isBlank() )
            n = Node.ANY;
        if ( n.isVariable() )
            n = Node.ANY;
        if ( ! n.isURI() && ! n.isLiteral() )
            Log.warn(LabelsIndex.class, "Not a valid in a pattern:: "+n);
        return n;
    }

    private void clear() {
        exact.clear();
        SP.clear();
        S.clear();
        P.clear();
        ANY.clear();
    }
}
