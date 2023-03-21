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

import static java.lang.String.format;

import io.telicent.jena.abac.core.DatasetGraphABAC;
import org.apache.jena.atlas.web.MediaType;
import org.apache.jena.fuseki.servlets.ActionLib;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.DatasetGraph;

/**
 * A Fuseki action to get the labels graph for a {@link DatasetGraphABAC}.
 */
public class ABAC_Labels extends ActionService implements ABAC_Processor {

    public ABAC_Labels() {}

    @Override
    public void execOptions(HttpAction action) {
        ActionLib.doOptionsGetPost(action);
        ServletOps.success(action);    }

    // Not supported - depends on query and body.
    @Override public void execHead(HttpAction action) { super.execHead(action); }

    // Accept GET and POST.
    @Override public void execGet(HttpAction action) {
        executeLifecycle(action);
    }

    @Override public void execPost(HttpAction action) {
        executeLifecycle(action);
    }

    @Override
    public void validate(HttpAction action) {
        DatasetGraph dsg = action.getDataset();
        if ( !( dsg instanceof DatasetGraphABAC ) ) {
            // Null or not DatasetGraphAuthz
            ServletOps.errorBadRequest("Not a ABAC dataset");
            return;
        }
    }

    @Override
    public void execute(HttpAction action) {
        DatasetGraph dsg = action.getDataset();
        DatasetGraphABAC dsgz = (DatasetGraphABAC)dsg;
        Graph graph = dsgz.labelsStore().getGraph();

        try {
            action.beginRead();
            MediaType mediaType = ActionLib.contentNegotationRDF(action);
            Lang lang = RDFLanguages.contentTypeToLang(mediaType.getContentTypeStr());
            if ( lang == null )
                lang = RDFLanguages.TURTLE;
            if ( action.verbose )
                action.log.info(format("[%d]   Labels: Content-Type=%s, Charset=%s => %s",
                                       action.id, mediaType.getContentTypeStr(), mediaType.getCharset(), lang.getName()));
            ActionLib.setCommonHeaders(action);
            ActionLib.graphResponse(action, graph, lang);

            // Use the preferred MIME type.
            ActionLib.graphResponse(action, graph, lang);
            ServletOps.success(action);
        } catch (JenaException ex) {
            ServletOps.errorOccurred(ex);
        } finally { action.endRead(); }
    }
}
