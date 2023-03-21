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

import java.util.function.Consumer;

import org.apache.jena.fuseki.patch.PatchApplyService;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.SPARQL_Update;
import org.apache.jena.riot.WebContent;

/**
 * Ingest point for ABAC changes (data loading, RDF Patch and SPARQL Update).
 * <p>
 * This replaces the processor for {@link Operation#Upload}) which is RDF data only.
 */
public class ABAC_ChangeDispatch extends ActionService implements ABAC_Processor {

    // Having a ABAC-specific dispatcher control data change is more flexible
    // in the way RDF Patch and SPARQL update interact with labels.
    //
    // Just overriding operation processors doesn't give the same opportunities for
    // control or operation introspection when mixing AABC and non-ABAC datasets
    // in one server at the cost of duplicating dispatch logic.
    //
    // Eventually, this may be unnecessary. For now, the indirection is put in place
    // as future-proofing.

    private ABAC_DataLoader dataLoader = new ABAC_DataLoader();

    private PatchApplyService patchHandler = new PatchApplyService();
    private SPARQL_Update updateHandler = new SPARQL_Update();

    public ABAC_ChangeDispatch() {}

    @Override
    public void execPost(HttpAction action) { executeLifecycle(action); }

    @Override
    public void validate(HttpAction action) {
        redirectByContentType(action, updateHandler::execute, patchHandler::execute, dataLoader::execute);
    }

    @Override
    public void execute(HttpAction action) {
        redirectByContentType(action, updateHandler::validate, patchHandler::validate, dataLoader::validate);
    }

    private void redirectByContentType(HttpAction httpAction,
                                       Consumer<HttpAction> actionUpdate,
                                       Consumer<HttpAction> actionPatch,
                                       Consumer<HttpAction> actionDefault) {
        String ct = httpAction.getRequestContentType();
        if ( ct != null ) {
            if ( WebContent.contentTypeSPARQLUpdate.equalsIgnoreCase(ct) ) {
                actionUpdate.accept(httpAction);
                return;
            }
            if ( WebContent.contentTypePatch.equalsIgnoreCase(ct) ||
                 WebContent.contentTypePatchThrift.equalsIgnoreCase(ct)) {
                actionPatch.accept(httpAction);
                return;
            }
        }
        actionDefault.accept(httpAction);
    }
}