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

import java.util.List;
import java.util.Locale;

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.StreamSplitter;
import io.telicent.jena.abac.core.VocabAuthz;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.fuseki.servlets.ActionErrorException;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.apache.jena.fuseki.system.DataUploader;
import org.apache.jena.fuseki.system.UploadDetails;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.TxnType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.web.HttpSC;

/**
 * Loader for data with {@code Security-Label} header
 *  or as a TriG with graph {@code authz:labels}
 * which is {@code http://telicent.io/security#labels}
 * and constant {@link VocabAuthz#graphForLabels}).
 */
public class ABAC_DataLoader extends ActionService implements ABAC_Processor {

    public ABAC_DataLoader() {}

    @Override
    public void execPost(HttpAction action) { executeLifecycle(action); }

    @Override
    public void validate(HttpAction action) {
        // No query string
        // Recognized Content-type.
        String ct = action.getRequestContentType();
        if ( ct == null ) {
            FmtLog.warn(action.log, "[%d] No Content-Type", action.id);
            ServletOps.error(HttpSC.BAD_REQUEST_400, "No Content-Type");
        }
        Lang lang = RDFLanguages.contentTypeToLang(ct);
        if ( lang == null ) {
            FmtLog.warn(action.log, "[%d] Bad Content-Type: %s", action.id, ct);
            ServletOps.error(HttpSC.UNSUPPORTED_MEDIA_TYPE_415, "Bad Content-Type: "+ct);
        }

        ct = ct.toLowerCase(Locale.ROOT);

        if ( Lang.TRIG.equals(lang) || Lang.NQUADS.equals(lang) ) {
            // OK
        } else if ( RDFLanguages.isTriples(lang) ) {
            // Must have security label.
            boolean isLabelled = action.getRequestHeader(ServerABAC.hSecurityLabel) != null;
            if ( ! isLabelled ) {
                FmtLog.warn(action.log, "[%d] Bad Content-Type: %s (triples formats must include a \"SecurityLabel\" header)", action.id, ct);
                ServletOps.error(HttpSC.UNSUPPORTED_MEDIA_TYPE_415, "Bad Content-Type: "+ct);
            }
        } else {
            // Not a supported language.
            FmtLog.warn(action.log, "[%d] Bad Content-Type: %s", action.id, ct);
            ServletOps.error(HttpSC.UNSUPPORTED_MEDIA_TYPE_415, "Bad Content-Type: "+ct);
        }

        DatasetGraph dsg =  action.getDataset();
        if ( ! ( dsg instanceof DatasetGraphABAC ) ) {
            FmtLog.warn(action.log, "[%d] Dataset '%s' does not support ABAC security labelling.", action.id, action.getDatasetName());
            ServletOps.error(HttpSC.BAD_REQUEST_400, "This dataset does not support ABAC security labelling.");
            return;
        }
    }

    @Override
    public void execute(HttpAction action) {
        DatasetGraph dsg = action.getDataset();
        if ( ! ( dsg instanceof DatasetGraphABAC ) ) {
            // Should have been caught in validate.
            FmtLog.error(action.log, "[%d] This dataset does not support ABAC security labelling.", action.id);
            ServletOps.error(HttpSC.BAD_REQUEST_400, "This dataset does not support ABAC security labelling.");
            return;
        }

        action.begin(TxnType.WRITE);
        try {
            DatasetGraphABAC dsgz = (DatasetGraphABAC)dsg;
            String ct = action.getRequestContentType();
            //long len = action.getRequestContentLengthLong();

            String dftSecuritysLabel = action.getRequestHeader(ServerABAC.hSecurityLabel);
            List<String> dataDftLabels = parseAttributeList(dftSecuritysLabel);
            if ( dataDftLabels != null )
                FmtLog.info(action.log, "[%d] Security-Label %s", action.id, dataDftLabels);
            else
                // Dataset default will apply at use time.
                FmtLog.info(action.log, "[%d] Dataset default label: %s", action.id, dsgz.getDefaultLabel());

            StreamRDF rdfData = StreamRDFLib.dataset(dsgz.getBase());
            // Get all the labels - as they may come first, we need to collect them
            // together, then process them before the txn commit.
            Graph labelsGraph = GraphFactory.createDefaultGraph();

            StreamRDF stream = new StreamSplitter(rdfData, labelsGraph, dataDftLabels);

            UploadDetails details = DataUploader.incomingData(action, stream);
            applyLabels(dsgz, labelsGraph);
            action.commit();
            ServletOps.uploadResponse(action, details);
        }
        catch (ActionErrorException ex) { action.abortSilent(); throw ex; }
        catch (Throwable ex) {
            action.abortSilent();
            ServletOps.errorOccurred(ex);
            return;
        }
    }

    private List<String> parseAttributeList(String securityLabelsList) {
        if ( securityLabelsList == null )
            return null;
        List<AttributeExpr> x = AE.parseExprList(securityLabelsList);
        return AE.asStrings(x);
    }

    private static void applyLabels(DatasetGraphABAC dsgz, Graph labelsGraph) {
        if ( labelsGraph != null && ! labelsGraph.isEmpty() )
            dsgz.labelsStore().add(labelsGraph);
    }
}