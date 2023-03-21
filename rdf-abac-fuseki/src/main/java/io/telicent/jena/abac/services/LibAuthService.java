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

import java.util.List;

import javax.servlet.http.HttpServlet;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.fuseki.main.JettyServer;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.fuseki.servlets.ServletAction;
import org.slf4j.Logger;

/**
 * Support for services.
 */
public class LibAuthService {
    /**
     * Servlet path for a service that is parameterized by some "{name}".
     * This returns the path upto the last "/", <b>without</b> trailing "*".
     * e.g. <code>/abc/def/{name} =&gt; /abc/def/</code>
     */
    public static String templateToPathName(String template) {
        if ( ! template.contains("{") )
            Log.warn(AttributeService.class, "Suspicious template (no '{'): "+template);
        if ( ! template.contains("{") )
            Log.warn(AttributeService.class, "Suspicious template (no '}'): "+template);
        int idx = template.lastIndexOf("/");
        if ( idx <= 0 ) {
            Log.warn(AttributeService.class, "Suspicious template (no slash): "+template);
            return template;
        }
        // Path, up to and including the final /
        String prefix = template.substring(0, idx+1);
        return prefix;
    }

    /** Get the value from a URI by template */
    public static String templateGetByName(String uri, String template, String name) {
        // Simple case after last slash.
        int idx = template.lastIndexOf("/");
        if ( idx <= 0 ) {
            Log.warn(AttributeService.class, "Suspicious template (no slash): "+template);
            return template;
        }
        // Path, up to and including the final /
        String value = uri.substring(idx+1);
        return value;
    }

    /** Add a path to  base URL */
    public static String serviceURL(String serverURL, String path) {
        if ( serverURL.endsWith("/") )
            serverURL = serverURL.substring(0, serverURL.length()-1);
        if ( ! path.startsWith("/") )
            path = "/"+path;
        return serverURL+path;
    }

    public static String run(int port, Logger log, List<Pair<String, ActionService>> services) {
        JettyServer.Builder builder = JettyServer.create().port(port);
        for ( Pair<String, ActionService> pair : services ) {
            String path = pair.getLeft();
            ActionService actionService = pair.getRight();
            HttpServlet servlet = new ServletAction(actionService, log);
            builder.addServlet(path, servlet);
        }
        JettyServer jettyServer = builder.build().start();
        String lookupURL = "http://localhost:"+jettyServer.getPort()+"/";
        return lookupURL;
    }

}
