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

package io.telicent.jena.abac.fuseki;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import io.telicent.jena.abac.core.DatasetGraphABAC;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.FusekiTestLib;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.exec.QueryExec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test Fuseki + FMod_ABAC
 */
public class TestServer_FMod_ABAC {
    static {
        FusekiLogging.setLogging();
    }

    private static String DIR = "src/test/files/server/";

    private static String PREFIXES = StrUtils.strjoinNL
            ("PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
            ,"PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>"
            ,"PREFIX sh:      <http://www.w3.org/ns/shacl#>"
            ,"PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>"
            ,"PREFIX : <http://example/>"
            ,""
             );

    static FusekiModule moduleABAC;

    // ----
    // This ought to go somewhere! Undo in TestRDFLinkRemote.
    // "solves the problem that src/test/resources/log4j2.properties can be loaded
    // from the main classpath before the test resources.
    private static String[] loggers = {
        Fuseki.serverLogName,
        Fuseki.actionLogName,
        Fuseki.requestLogName,
        Fuseki.adminLogName,
        "org.eclipse.jetty"
    };

    private static void silentAll(Runnable action) {
        silentAll(action, loggers, "ERROR");
    }

    private static void silentAll(Runnable action, String[] loggers, String runLevel) {
        Map<String, String> levels = new HashMap<>();
        for ( String logger : loggers ) {
            levels.put(logger, LogCtl.getLevel(logger));
            //LogCtl.disable(logger);
            LogCtl.setLevel(logger, runLevel);
        }
        try {
            action.run();
        } finally {
            levels.forEach(LogCtl::setLevel);
        }
    }
    // ----

    @BeforeAll
    public static void setup() {
//        Function<HttpAction, String> getUserFunction = a->"u1";
//        moduleABAC = FMod_ABAC.testSetup(getUserFunction);
        moduleABAC = new FMod_ABAC();
        FusekiModules.add(moduleABAC);
    }

    @AfterAll
    public static void cleanup() {
        FusekiModules.remove(moduleABAC);
    }

    @Test public void build_labels() {
        FusekiServer server = FusekiServer.create().port(0).parseConfigFile(DIR+"server-labels/config-labels.ttl").build();
        DatasetGraph dsg0 = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC dsgz = (DatasetGraphABAC)dsg0;
        assertNotNull(dsgz.labelsStore());
        assertFalse(dsgz.labelsStore().isEmpty());

        assertNull(dsgz.getAccessAttributes());
        assertNotNull(dsgz.getDefaultLabel());
        assertTrue(dsgz.getDefaultLabel().equals("default"));
    }

    @Test public void build_run_server() {
        FusekiServer server = FusekiServer.create().port(0).parseConfigFile(DIR+"server-labels/config-labels.ttl").build();
        DatasetGraph dsg0 = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC dsgz = (DatasetGraphABAC)dsg0;
        server.start();
        try {
            String URL="http://localhost:"+server.getPort()+"/ds";
            String URL_BASE="http://localhost:"+server.getPort()+"/base";

            // No user at all -> 400.
            silentAll(()-> {
                FusekiTestLib.expectQuery400( ()-> QueryExec.service(URL).query("ASK{}").ask());
            });
        } finally { server.stop(); }
    }

    // Check that a ABAC dataset cause override of the action processors for operations in the fuseki service.
    // The config uses fuseki:query/fuseki:upload, not authz:query/authz:upload,
    @Test public void build_run_server_override() {
        FusekiServer server = FusekiServer.create().port(0).parseConfigFile(DIR+"config-server-plain.ttl").build();
        DatasetGraph dsg0 = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC dsgz = (DatasetGraphABAC)dsg0;
        server.start();
        try {
            String URL="http://localhost:"+server.getPort()+"/ds";
            String URL_BASE="http://localhost:"+server.getPort()+"/base";

            // No user at all -> 400.
            silentAll(()-> {
                FusekiTestLib.expectQuery400( ()-> QueryExec.service(URL).query("ASK{}").ask());
            });

            // But can access the other dataset without any security.
            QueryExec.service(URL_BASE).query("ASK{}").ask();
        } finally { server.stop(); }
    }
}
