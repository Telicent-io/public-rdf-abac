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

import static io.telicent.jena.abac.services.LibAuthService.serviceURL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.core.Attributes;
import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.services.AttributeService;
import io.telicent.jena.abac.services.SimpleAttributesStore;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.exec.RowSetOps;
import org.apache.jena.sparql.exec.http.DSP;
import org.apache.jena.sparql.exec.http.QueryExecHTTPBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests for a remote attribute store. */
public class TestAttributesStoreRemote {

    private static String DIR = "src/test/files/integration";

    protected static String level = null;

    @BeforeAll
    public static void beforeAll() {
        // See also src/test/resources/log4j2.properties
        FusekiLogging.setLogging();
        level = LogCtl.getLevel(ABAC.AttrLOG);
        LogCtl.disable(ABAC.AttrLOG);
    }

    @AfterAll
    public static void afterAll() {
        LogCtl.setLevel(ABAC.AttrLOG, level);
    }

    @Test public void integration_1() {
        withFilterLoggering(false,
                            ()->runTest("config-java-services.ttl", 2));
    }

    // Missing matches are hard to find without filter logging, but that can be verbose to have on.
    // See also src/test/resources/log4j2.properties
    private void withFilterLoggering(boolean value, Runnable action) {
        Labels.setLabelFilterLogging(value);
        try {
            action.run();
        } finally { Labels.setLabelFilterLogging(false); }
    }

    @Test public void integration_2() {
        withFilterLoggering(false,
                            ()->runTest("config-no-hierarchies.ttl", 1));
    }

    private static void runTest(String configFile, int expected) {
        // Remote Attribute Store
        Graph g = RDFParser.source(DIR+"/attribute-store.ttl").toGraph();
        AttributesStore attrStore = Attributes.buildStore(g);
        String mockServerURL = SimpleAttributesStore.run(0, attrStore);

        String attributeStoreBaseURL = mockServerURL;
        String lookupUserAttribesURL = serviceURL(attributeStoreBaseURL, AttributeService.lookupUserAttributeTemplate);
        String lookupHierarchAttribesURL = serviceURL(attributeStoreBaseURL, AttributeService.lookupHierarchyTemplate);

        System.setProperty("USER_ATTRIBUTES_URL", lookupUserAttribesURL);
        System.setProperty("ABAC_HIERARCHIES_URL", lookupHierarchAttribesURL);

        FusekiServer server = FusekiServer
                .construct("--conf", DIR+"/"+configFile)
                .start();
        try {
            int port = server.getHttpPort();
            String URL = "http://localhost:" + port + "/ds";
            DSP.service(URL+"/upload").POST(DIR+"/data-hierarchies.trig");

            RowSet rs = query(URL, "SELECT * {?s ?p ?o}", "user1@email");
            //RowSetOps.out(rs);
            // With hierarchies: 2
            // Without hierarchies: 1
            long x = RowSetOps.count(rs);
            assertEquals(expected, x);
        } finally {
            if ( server != null )
                server.stop();
        }
    }

    public static RowSet query(String url, String queryString, String user) {
        String qs = queryString;
        RowSet rs1 =
                QueryExecHTTPBuilder.service(url)
                .httpHeader("Authorization", "Bearer user:"+user)
                .query(qs)
                .select();
        return rs1;
    }
}
