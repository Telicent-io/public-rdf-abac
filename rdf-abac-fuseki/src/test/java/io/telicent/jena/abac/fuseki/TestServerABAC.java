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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Authenticator;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.telicent.jena.abac.core.DatasetGraphABAC;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.FusekiTestLib;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.graph.Graph;
import org.apache.jena.http.HttpRDF;
import org.apache.jena.http.auth.AuthLib;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.exec.RowSetOps;
import org.apache.jena.sparql.exec.RowSetRewindable;
import org.apache.jena.sparql.exec.http.DSP;
import org.apache.jena.sparql.exec.http.GSP;
import org.apache.jena.sparql.exec.http.QueryExecHTTPBuilder;
import org.junit.jupiter.api.Test;

/**
 * Test Fuseki+ABAC
 */
public class TestServerABAC {
    static {
        FusekiLogging.setLogging();
    }

    // Some tests have authz:tripleDefaultLabels "*";
    // some do not.
    // u1 can see 3 triples with this and 2 without it
    // u3 can see 1 triple1 with this and 0 without it

    private static String DIR = "src/test/files/server/";

    private static String PREFIXES = StrUtils.strjoinNL
            ("PREFIX : <http://example/>", "");
//            ("PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
//            ,"PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>"
//            ,"PREFIX sh:      <http://www.w3.org/ns/shacl#>"
//            ,"PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>"
//            ,"PREFIX : <http://example/>"
//            ,""
//             );

    // ----

    private static String[] loggersWarn = {
        Fuseki.serverLogName,
        Fuseki.adminLogName,
        Fuseki.requestLogName,
        "org.eclipse.jetty"
    };

    private static String[] loggersAction = {
        Fuseki.actionLogName
    };

    private static void silentAll(Runnable action) {
        silent("WARN", loggersWarn, action);
        silent("OFF",  loggersAction, action);
    }

    private static void silent(String runLevel, String[] loggers, Runnable action) {
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

    // ----

//    @BeforeAll
//    public static void setup() {}
//
//    @AfterAll
//    public static void cleanup() {}

    private static FusekiServer server(String config) {
        return FusekiServer.create().port(0).parseConfigFile(DIR+config).build();
    }

    @Test public void build() {
        FusekiServer server = server("config-server.ttl");
        DatasetGraph dsg0 = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC dsgz = (DatasetGraphABAC)dsg0;
        assertNotNull(dsgz.labelsStore());
        assertTrue(dsgz.labelsStore().isEmpty());
        server.start();
        server.stop();
    }

    // No load only tests - can't determine the success or failure without a query.

    @Test public void query_u1() {
        FusekiServer server = server("config-server.ttl");
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        try {
            load(server);
            query(URL, "u1", 3);
        } finally { server.stop(); }
    }

    @Test public void query_u1_u2() {
        FusekiServer server = server("config-server.ttl");
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        try {
            load(server);
            query(URL, "u1", 3);
            query(URL, "u2", 2);
        } finally { server.stop(); }
    }

    @Test public void query_anon() {
        FusekiServer server = server("config-server.ttl");
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        try {
            load(server);
            silent("OFF", loggersAction,
                   ()->FusekiTestLib.expectQuery403( ()-> query(URL, "anon", 0) ) );
        } finally { server.stop(); }
    }

    @Test public void query_dft_access() {
        FusekiServer server = server("config-server-dft-label.ttl");
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        try {
            load(server);
            query(URL, "u1", PREFIXES+"SELECT * { ?s :q ?o }", 0);
            query(URL, "u2", PREFIXES+"SELECT * { ?s :q ?o }", 1);
        } finally { server.stop(); }
    }

    @Test public void query_access() {
        FusekiServer server = server("config-server-access.ttl");
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        try {
            // No data, no labels.
            // u1 can not access
            silent("ERROR", loggersAction,
                   ()->FusekiTestLib.expectQuery403( ()-> query(URL, "u1", 0) ) );
            // u2 can access
            query(URL, "u2", 0);
        } finally { server.stop(); }
    }

    @Test public void gsp_r_1() {
        FusekiServer server = server("config-server-gspr.ttl");
        server.start();
        String baseURL = "http://localhost:"+server.getPort();
        DSP.service(baseURL+"/ds/upload").POST(DIR+"data-and-labels.trig");

        try {
            // Try all the endpoints.
            Graph g11 = gspGET(baseURL+"/ds", "u1", 3);
            Graph g12 = gspGET(baseURL+"/ds/gsp-r-plain", "u1", 3);
            Graph g13 = gspGET(baseURL+"/ds/gsp-r-authz", "u1", 3);
            assertTrue(g11.isIsomorphicWith(g12));
            assertTrue(g11.isIsomorphicWith(g13));

            Graph g2 = gspGET(baseURL+"/ds", "u2", 2);
            assertFalse(g11.isIsomorphicWith(g2));
            // u3 can access but can only see the unlabelled triple.
            gspGET(baseURL+"/ds/gsp-r-plain", "u3", 1);
        } finally { server.stop(); }
    }

    @Test public void gsp_r_2() {
        FusekiServer server = server("config-server-gspr.ttl");
        server.start();
        String baseURL = "http://localhost:"+server.getPort();
        DSP.service(baseURL+"/ds/upload").POST(DIR+"data-and-labels.trig");

        try {
            Graph g1 = gspGET(baseURL+"/ds", "u1", 3);
            Graph g2 = gspGET(baseURL+"/ds", "u2", 2);
            assertFalse(g1.isIsomorphicWith(g2));
            // u3 can access but can only see the unlabelled triple.
            gspGET(baseURL+"/ds/gsp-r-plain", "u3", 1);
        } finally { server.stop(); }
    }

    // -- Check security applied for plain (fuseki:) forms and authz: forms.

    // Configuration: fuseki:query/fuseki:upload forms.
    @Test public void config_override_plain() {
        config_override("config-server-plain.ttl");
    }

    // Configuration: preferred form (fuseki:query, authz:upload)
    @Test public void config_override_preferred() {
        config_override("config-server.ttl");
    }

    private void config_override(String configFile) {
        FusekiServer server = server(configFile);
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        DSP.service(URL+"/upload").POST(DIR+"data-and-labels.trig");
        // u2 can access
        query(URL, "u2", 2);
        gspGET(URL, "u2", 2);
        // u3 can access and can see only the unlabelled label.
        query(URL, "u3", 1);
        gspGET(URL, "u3", 1);

        // ---- The plain dataset
        String URL2 = "http://localhost:"+server.getPort()+"/base";
        boolean b = QueryExecHTTPBuilder.service(URL2).query("ASK {}").ask();
    }

    // Configuration: authz:query/authz:upload forms.
    // Custom operations don't allow for multi-operation dispatch on an endpoint
    // (only supported for the SPARQL standard forms).
    // Must use endpoint with a single operations.
    @Test public void config_override_custom_forms() {
        FusekiServer server = server("config-server-authz.ttl");
        server.start();
        try {
            String baseURL = "http://localhost:"+server.getPort()+"/ds";
            DSP.service(baseURL+"/upload").POST(DIR+"data-and-labels.trig");

            // u2 can access
            query(baseURL+"/query", "u2", 1);
            gspGET(baseURL+"/gsp-r", "u2", 1);
            // u3 can access and can see only the unlabelled label but that is "deny"
            query(baseURL+"/query", "u3", 0);
            gspGET(baseURL+"/gsp-r", "u3", 0);

            // ---- The plain dataset
            String URL2 = "http://localhost:"+server.getPort()+"/base";
            boolean b = QueryExecHTTPBuilder.service(URL2).query("ASK {}").ask();
        } finally { server.stop(); }
    }

    // Authentication using Jetty for login.
    @Test public void config_server_auth() {
        FusekiServer server = FusekiServer.create().port(0)
                .parseConfigFile(DIR+"config-jetty-auth.ttl")
                // In config file.
                //   .passwordFile(DIR+"jetty-passwd")
                //   .auth(AuthScheme.BASIC)
                .build();
        server.start();
        try {
            String URL = "http://localhost:"+server.getPort()+"/ds";
            // Only u1 is in the password file.

            HttpClient httpClient_u1 = createHttpClient("u1",  "pw1");
            HttpClient httpClient_u2 = createHttpClient("u2",  "pw2");

            DSP.service(URL+"/upload").httpClient(httpClient_u1).POST(DIR+"data-and-labels.trig");
            // u2 can not access the server
            FusekiTestLib.expectQuery401(()->query(URL, httpClient_u2, "u2", 3));
            // u1 can access
            query(URL, httpClient_u1, "u1", 2);
        } finally { server.stop(); }
    }


    @Test public void get_labels() {
        FusekiServer server = server("config-server-authz.ttl");
        server.start();
        try {
            String URL="http://localhost:"+server.getPort()+"/ds";
            Graph g1 = HttpRDF.httpGetGraph(URL+"/labels", null);
            assertTrue(g1.isEmpty(), "Expected no labels triples in empty dataset");

            load(server);

            Graph g2 = HttpRDF.httpGetGraph(URL+"/labels", null);
            assertFalse(g2.isEmpty(), "Expected labels triples");
        } finally { server.stop(); }
    }

    private static final String DatabaseArea = "target/databases";

    @Test public void server_restart() {
        // Clear databases
        FileOps.ensureDir(DatabaseArea);
        FileOps.clearAll(DatabaseArea);

        {
            // Empty server
            FusekiServer server1 = server("config-server-persistent.ttl");
            server1.start();
            try {
                String URL="http://localhost:"+server1.getPort()+"/ds";
                query(URL, "u1", 0);
                query(URL, "u3", 0);
                load(server1);
                query(URL, "u1", 2);
                query(URL, "u3", 0);
            } finally { server1.stop(); }
        }

        {
            // restart
            FusekiServer server2 = server("config-server-persistent.ttl");
            server2.start();
            try {
                String URL="http://localhost:"+server2.getPort()+"/ds";
                query(URL, "u1", 2);
                query(URL, "u3", 0);
            } finally { server2.stop(); }
        }
    }

    @Test public void server_all() {
        FusekiServer server = server("all/config-all.ttl");
        server.start();
        try {

            String URL="http://localhost:"+server.getPort()+"/ds";
            Graph g1 = HttpRDF.httpGetGraph(URL+"/labels", null);
            assertTrue(g1.isEmpty(), "Expected no labels triples in empty dataset");
            // Check nothing.
            query(URL, "u1", 0);

            load(URL+"/upload", DIR+"all/data-labels-all.trig");
            Graph g2 = HttpRDF.httpGetGraph(URL+"/labels", null);
            assertFalse(g2.isEmpty(), "Expected labels triples in label graph");

            query(URL, "u1", 7);
            query(URL, "u2", 4);
            query(URL, "u3", 1);
        } finally { server.stop(); }
    }

    static String config = """
    ## Configuration using authz: forms.

    PREFIX :        <#>
    PREFIX fuseki:  <http://jena.apache.org/fuseki#>
    PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX ja:      <http://jena.hpl.hp.com/2005/11/Assembler#>
    PREFIX tdb2:    <http://jena.apache.org/2016/tdb#>

    PREFIX authz:   <http://telicent.io/security#>

    :service1 rdf:type fuseki:Service ;
        fuseki:name "ds" ;
        ## Must be fuseki:{operation} for dynamic dispatch.
        fuseki:endpoint [ fuseki:operation fuseki:query ] ;
        fuseki:endpoint [ fuseki:operation fuseki:gsp-r ] ;

        ## Same but as named services
        fuseki:endpoint [ fuseki:operation authz:query ; fuseki:name "query" ] ;
        fuseki:endpoint [ fuseki:operation authz:gsp-r ; fuseki:name "gsp-r" ] ;

        fuseki:endpoint [ fuseki:operation authz:upload ; fuseki:name "upload" ] ;
        fuseki:endpoint [ fuseki:operation authz:labels ; fuseki:name "labels" ] ;

        fuseki:dataset :dataset ;
        .

    ## ABAC Dataset:
    :dataset rdf:type authz:DatasetAuthz ;
        authz:labels :databaseLabels ;
        authz:attributes <file:src/test/files/server/all/attribute-store-all.ttl> ;

        ##authz:accessAttributes      "";
        authz:tripleDefaultLabels     "*" ;

        authz:dataset :datasetBase;
        .

    # Data: Transactional in-memory dataset.
    :datasetBase rdf:type ja:MemoryDataset .

    # Labels.
    :databaseLabels rdf:type ja:MemoryDataset .
    """;

    @Test public void load_labelled_data() {

//        // Mock attribute store.
//        Graph g = RDFParser.source(DIR+"/attribute-store-all.ttl").toGraph();
//        AttributesStore attrStore = Attributes.buildStore(g);
//        String mockServerURL = MockAttributesStore.run(0, attrStore);
//        // Eval server.

        Graph configGraph = RDFParser.fromString(config).lang(Lang.TTL).toGraph();

        FusekiServer server = FusekiServer.create().port(0)
                //.parseConfigFile(DIR+"all/config-all.ttl")
                .parseConfig(ModelFactory.createModelForGraph(configGraph))
                .build();
        server.start();
        String URL="http://localhost:"+server.getPort()+"/ds";
        try {
            String payload1 = """
            Content-type: application/trig

            PREFIX : <http://example/>

                :s :p1 123 .
                :s :p2 456 .
                :s :p2 789 .

                :s :q "No label" .

                :s1 :p1 1234 .
                :s1 :p2 2345 .

                :x :public "abc" .
                :x :public "def" .

                :x :confidential "C-abc" .
                :x :sensitive    "S-abc" .
                :x :private      "P-abc" .

                PREFIX authz: <http://telicent.io/security#>

                GRAPH authz:labels {

                    [ authz:pattern ':s :p1 123'  ; authz:label "level-1" ] .

                    ## Multiple labels.
                    [ authz:pattern ':s :p2 456'  ; authz:label "manager", "level-1" ] .
                    [ authz:pattern ':s :p2 789'  ; authz:label "manager"  ] .

                    [ authz:pattern ':s1 :p1 ANY' ; authz:label "manager"  ] .
                    [ authz:pattern ':s1 :p2 ANY' ; authz:label "engineer" ] .

                    [ authz:pattern ':x  :public        ANY' ; authz:label "status=public" ] .
                    [ authz:pattern ':x  :confidential  ANY' ; authz:label "status=confidential" ] .
                    [ authz:pattern ':x  :sensitive     ANY' ; authz:label "status=sensitive" ] .
                    [ authz:pattern ':x  :private       ANY' ; authz:label "status=private" ] .
                }
                    """;
            PlayLib.sendStringHTTP(URL+"/upload", payload1);
            //load(URL+"/upload", DIR+"all/data-labels-all.trig");

            query(URL, "u1", 7);
            query(URL, "u2", 4);
            query(URL, "u3", 1);

        } finally { server.stop(); }
    }

    private HttpClient createHttpClient(String user, String password) {
        Authenticator authenticator1 = AuthLib.authenticator(user, password);
        return HttpClient.newBuilder()
                .authenticator(authenticator1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private void load(FusekiServer server) {
        String URL = "http://localhost:"+server.getPort()+"/ds";
        String uploadURL = URL+"/upload";
        load(uploadURL, DIR+"data-and-labels.trig");
    }

    private void load(String uploadURL, String filename) {
        DSP.service(uploadURL).POST(filename);
    }

    private void query(String url, String user, int expectedCount) {
        String queryString = "SELECT * { ?s ?p ?o }";
        query(url, user, queryString, expectedCount);
    }

    private void query(String url,  HttpClient httpClient, String user, int expectedCount) {
        String queryString = "SELECT * { ?s ?p ?o }";
        queryWithHttpClient(url, httpClient, user, queryString, expectedCount);
    }

    private void queryDev(String url, String user) {
        String queryString = "SELECT * { ?s ?p ?o }";
        queryDev(url, user, queryString);
    }

    private void queryDev(String url, String user, String queryString) {
        // == ABAC Query
        RowSet rowSet =
                QueryExecHTTPBuilder.service(url)
                .query(queryString)
                .httpHeader("Authorization", "Bearer user:"+user)
                .select();
        RowSetOps.out(rowSet);
    }

    private void query(String url, String user, String queryString, int expectedCount) {
        // == ABAC Query
        RowSetRewindable rowSet =
                QueryExecHTTPBuilder.service(url)
                .query(queryString)
                .httpHeader("Authorization", "Bearer user:"+user)
                .select()
                .rewindable();
        long x = RowSetOps.count(rowSet);
        if ( expectedCount != x ) {
            rowSet.reset();
            RowSetOps.out(System.out, rowSet);
        }

        assertEquals(expectedCount, x);
    }

    private Graph gspGET(String URL, String user, int expectedCount) {
        Graph graph = GSP.service(URL)
            .defaultGraph()
            .httpHeader("Authorization", "Bearer user:"+user)
            .GET();
        int x = graph.size();
        assertEquals(expectedCount, x);
        return graph;
    }

    private void queryWithHttpClient(String url, HttpClient httpClient, String user, String queryString, int expectedCount) {
        // == ABAC Query
        RowSet rowSet =
                QueryExecHTTPBuilder.service(url)
                .httpClient(httpClient)
                .query(queryString)
                .select();
        long x = RowSetOps.count(rowSet);
        assertEquals(expectedCount, x);
    }
}
