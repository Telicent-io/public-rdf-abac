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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.HierarchyGetter;
import org.apache.jena.atlas.lib.ListUtils;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.util.IsoMatcher;
import org.slf4j.Logger;

public class ABACTests {

    public static void runTest(String filename, int count) {
        DatasetGraph aio = RDFDataMgr.loadDatasetGraph(filename);
        DatasetGraphABAC dsgz = BuildAIO.setupByTriG(aio, null);
        Graph expected = aio.getGraph(VocabAuthzTest.graphForTestResult);

        // == Request
        String user = "u1";
        HierarchyGetter function = (a)->dsgz.attributesStore().getHierarchy(a);
        DatasetGraph dsgr = ABAC.requestDataset(dsgz, dsgz.attributesForUser().apply(user), function);

        String queryString = "CONSTRUCT WHERE { ?s ?p ?o }";
        // === Result
        Graph actual = QueryExec.dataset(dsgr).query(queryString).construct();

        boolean b = IsoMatcher.isomorphic(expected, actual);
        if ( !b ) {
            System.out.println("** Expected");
            RDFDataMgr.write(System.out, expected, Lang.TTL);
            System.out.println("----");
            System.out.println("** Actual");
            RDFDataMgr.write(System.out, actual, Lang.TTL);
            System.out.println("----");
        }
        int x = actual.size();
        assertEquals(count, x);
        assertTrue("Not isomorphic", b);
    }

    public static void loggerAtLevel(Class<?> logger, String runLevel, Runnable action) {
        loggerAtLevel(logger.getName(), runLevel, action);
    }

    /**
     * Run an action with some loggers set to a temporary log level.
     */
    private /*public*/ static void loggerAtLevel(String logger, String runLevel, Runnable action) {
        // Risk of confusion of logger and level.
        Objects.requireNonNull(logger);
        Objects.requireNonNull(runLevel);
        String oldLevel = LogCtl.getLevel(logger);
        LogCtl.setLevel(logger, runLevel);
        try {
            action.run();
        } finally {
            LogCtl.setLevel(logger, oldLevel);
        }
    }

    /**
     * Run an action with some loggers set to a temporary log level.
     */
    public static void loggerAtLevel(Logger logger, String runLevel, Runnable action) {
        Objects.requireNonNull(logger);
        Objects.requireNonNull(runLevel);
        String oldLevel = LogCtl.getLevel(logger);
        LogCtl.setLevel(logger, runLevel);
        try {
            action.run();
        } finally {
            LogCtl.setLevel(logger, oldLevel);
        }
    }


    /**
     * Run an action with some loggers set to a temporary log level.
     */
    public static void silent(String runLevel, String[] loggers, Runnable action) {
        // Not String...loggers - action maybe be inline code.
        if ( loggers.length == 0 )
            System.err.println("Warning: Empty array of loggers passed to silent()");
        Map<String, String> levels = new HashMap<>();
        for ( String logger : loggers ) {
            levels.put(logger, LogCtl.getLevel(logger));
            LogCtl.setLevel(logger, runLevel);
        }
        try {
            action.run();
        } finally {
            levels.forEach(LogCtl::setLevel);
        }
    }

    public static <X> void assertEqualsUnordered(List<X> actual, List<X> expected) {
        boolean b = ListUtils.equalsUnordered(actual, expected);
        if ( !b ) {
            String msg = "Expected: "+expected+", Got: "+actual;
            fail(msg);
        }
    }
}
