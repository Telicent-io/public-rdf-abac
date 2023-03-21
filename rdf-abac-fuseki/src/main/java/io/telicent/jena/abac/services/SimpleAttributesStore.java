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

package io.telicent.jena.abac.services;

import static io.telicent.jena.abac.services.LibAuthService.templateGetByName;
import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.core.AttributesStore;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.fuseki.main.JettyServer;
import org.apache.jena.fuseki.servlets.ActionProcessor;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.apache.jena.riot.WebContent;
import org.apache.jena.web.HttpSC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One of the services used by rdf-abac is an external service to lookup user attributes and hierarchies.
 * This a Jetty server with two servlets:
 * <ul>
 * <li>User attributes : <code>/users/lookup/{user}</code></li>
 * <li>Hierarchies : <code>/hierarchies/lookup/{name}</code></li>
 * </li>
 * </ul>
 */
public class SimpleAttributesStore {
    private static Logger LOG = LoggerFactory.getLogger("io.telicent.jena.MockAS");
    private static String servletUserLookup = AttributeService.lookupUserAttributePath;
    private static String servletHierarchyLookup = AttributeService.lookupHierarchyPath;

    public static String run(int port, AttributesStore storage) {
        HttpServlet servlet1 = createLookupUserAttributeServlet(storage, LOG);
        HttpServlet servlet2 = createLookupHierarchyServlet(storage, LOG);

        JettyServer jettyServer = JettyServer.create()
            .port(port)
            .addServlet(AttributeService.lookupUserAttributeServletPathSpec, servlet1)
            .addServlet(AttributeService.lookupHierarchyServletPathSpec, servlet2)
            .build()
            .start();
        String lookupBaseURL = "http://localhost:"+jettyServer.getPort();
        LOG.info(format("MockAttributesStore: %s", lookupBaseURL));
        return lookupBaseURL;
    }

    static public HttpServlet createLookupUserAttributeServlet(AttributesStore storage, Logger logger) {
        logger = (logger == null) ? LOG : logger;
        return new ServletAction(new MockLookupUserActionProcessor(storage), logger);
    }

    static public HttpServlet createLookupHierarchyServlet(AttributesStore storage, Logger logger) {
        logger = (logger == null) ? LOG : logger;
        return new ServletAction(new MockLookupHierarchActionProcessor(storage), logger);
    }

    /** Send an error setting HTTP status code and with a JSON bobject body. */
    static void sendJsonError(HttpAction action, int httpStatusCode, int jsonErrorCode, String message) {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        JsonObject r = JSON.buildObject(builder->{
            builder.pair("code", jsonErrorCode);
            if ( message != null )
                builder.pair("message", message );
        });
        IndentedWriter bytesOut2 = new IndentedWriter(bytesOut);
        JSON.write(bytesOut2, r);
        byte[] bytes = bytesOut.toByteArray();
        try {
            ServletOutputStream out = action.getResponseOutputStream();
            action.setResponseStatus(httpStatusCode);
            action.setResponseContentType(WebContent.contentTypeJSON);
            action.setResponseContentLength(bytes.length);
            action.setResponseCharacterEncoding(WebContent.charsetUTF8);
            out.write(bytes);
            out.flush();
        } catch (IOException ex) { ServletOps.errorOccurred(ex); }
    }

    /**
     * Handler for <code>GET /users/lookup/{user}</code>.
     * Responses:
     * <ul>
     * <li><code>{ "attributes": [ string1, string2, ... ] }</code></li>
     * <li>Not found: 404 <code>{ "code": ; message: "" }</li>
     * </ul>
     */
    static class MockLookupUserActionProcessor implements ActionProcessor {
        private final AttributesStore storage;

        public MockLookupUserActionProcessor(AttributesStore storage) {
            this.storage = storage;
        }

        @Override
        public void execGet(HttpAction action) {
            String user = templateGetByName(action.getActionURI(),
                                            servletUserLookup,
                                            "user");
            if (user == null ) {
                action.log.error(format("[%d] User = null", action.id));
                /*ServletOps.*/sendJsonError(action, HttpSC.BAD_REQUEST_400, "No {user} in request");
                return;
            }
            AttributeValueSet lookup = storage.attributes(user);
            if ( lookup == null ) {
                action.log.info(format("[%d] User = %s not found", action.id, user));
                /*ServletOps.*/sendJsonError(action, HttpSC.NOT_FOUND_404, "User not found");
                return;
            }
            action.log.info(format("[%d] User = %s %s", action.id, user, lookup));

            JsonObject r = JSON.buildObject(builder->{
                builder.key("attributes");
                builder.startArray();
                lookup.attributeValues(attrValue-> builder.value(attrValue.asString()) );
                builder.finishArray();
            });
            action.log.info(JSON.toStringFlat(r));
            ServletOps.sendJson(action, r);
            ServletOps.success(action);
            return;
        }

        private static void sendJsonError(HttpAction action, int statusCode, String message) {
            SimpleAttributesStore.sendJsonError(action, statusCode, statusCode, message);
        }
    }

    /**
     * Handler for <code>GET /hierarchies/lookup/{name}</code>.
     * Responses:
     * <ul>
     * <li><code>{ "tiers": [ string1, string2, ... ] }</code></li>
     * <li>Not found: 404 <code>{ "code": ; message: "" }</li>
     * </ul>
     */
    static class MockLookupHierarchActionProcessor implements ActionProcessor {
        private final AttributesStore storage;

        public MockLookupHierarchActionProcessor(AttributesStore storage) {
            this.storage = storage;
        }

        @Override
        public void execGet(HttpAction action) {
            String name = templateGetByName(action.getActionURI(),
                                            servletHierarchyLookup,
                                            "name");
            if (name == null ) {
                action.log.error(format("[%d] Hierarchy = null", action.id));
                /*ServletOps.*/sendJsonError(action, HttpSC.BAD_REQUEST_400, "No {name} found");
                return;
            }
            Attribute attribute = new Attribute(name);
            Hierarchy lookup = storage.getHierarchy(attribute);
            if ( lookup == null ) {
                action.log.info(format("[%d] Hierarchy = %s not found", action.id, name));
                /*ServletOps.*/sendJsonError(action, HttpSC.NOT_FOUND_404, "Hierarchy not found");
                return;
            }
            action.log.info(format("[%d] Hierarchy = %s", action.id, lookup));
            JsonObject r = JSON.buildObject(builder->{
                builder.key("tiers");
                builder.startArray();
                lookup.values().forEach( valueTerm -> builder.value(valueTerm.asString()) );
                builder.finishArray();
            });
            action.log.info(JSON.toStringFlat(r));

            ServletOps.sendJson(action, r);
            ServletOps.success(action);
            return;
        }

        private static void sendJsonError(HttpAction action, int statusCode, String message) {
            SimpleAttributesStore.sendJsonError(action, statusCode, statusCode, message);
        }
    }
}
