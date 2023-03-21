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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.fuseki.ServerABAC.Vocab;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.FusekiException;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.FusekiServer.Builder;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.fuseki.server.*;
import org.apache.jena.fuseki.servlets.ActionProcessor;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;

/** Fuseki module for ABAC. */
public class FMod_ABAC implements FusekiModule {
    public static int LEVEL = 100;

    private final static Logger LOG = ABAC.AzLOG;
    private final Function<HttpAction, String> getUser;

    public FMod_ABAC() {
        this(ServerABAC.userForRequest());
    }

    private FMod_ABAC(Function<HttpAction, String> getUser) {
        this.getUser = getUser;
    }

    @Override
    public int level() { return LEVEL; }

    private static Collection<String> dataContentTypes = langContentTypes();

    private static Collection<String> langContentTypes() {
        if ( true )
            // Disable.
            return null;
        return List.of( WebContent.contentTypeTriG
                        , WebContent.contentTypeTriGAlt1
                        , WebContent.contentTypeTurtle
                        , WebContent.contentTypeTurtleAlt1
                        , WebContent.contentTypeNTriples
                        , WebContent.contentTypeNTriplesAlt
                        , WebContent.contentTypeNQuads
                        , WebContent.contentTypeJSONLD
                        , WebContent.contentTypeRDFProto
                        , WebContent.contentTypeRDFThrift
                        , WebContent.contentTypeRDFXML );

//        // Content-type for each language. Picks up junk.
//        Set<String> dataContentTypes = new HashSet<>();
//        RDFParserRegistry.registeredLangTriples().forEach(lang->dataContentTypes.add(lang.getHeaderString()));
//        RDFParserRegistry.registeredLangQuads().forEach(lang->dataContentTypes.add(lang.getHeaderString()));
//        System.err.println(dataContentTypes);
//        return dataContentTypes;
    }

    @Override
    public void start() {
       FmtLog.info(Fuseki.configLog, "ABAC Fuseki Module (%s)", SysABAC.VERSION);

        // Load operations and handlers for Fuseki.
        // Registration is in SysABAC.
        // Use if authz:query and authz:upload needed.
        // Normally, FMod_ABAC replaces the processors for the regular query and upload operations,
        // fuseki:query, fuseki:upload (for ABAC_ChangeDispatch)

        ActionService queryLabelsProc = new ABAC_SPARQL_QueryDataset(ServerABAC.userForRequest());
        ActionService gspReadLabelsProc = new ABAC_GSP_R(ServerABAC.userForRequest());
        ActionService loaderLabelsProc = new ABAC_ChangeDispatch();
        ActionService labelsGetterProc = new ABAC_Labels();

        OperationRegistry.get().register(Vocab.operationGSPRLabels, gspReadLabelsProc);
        OperationRegistry.get().register(Vocab.operationUploadABAC, loaderLabelsProc);
        OperationRegistry.get().register(Vocab.operationQueryLabels, queryLabelsProc);
        OperationRegistry.get().register(Vocab.operationGetLabels, labelsGetterProc);

        if ( dataContentTypes != null )
            // To allow multiple operations on the endpoint, need to register content types.
            dataContentTypes.forEach(ct-> OperationRegistry.get().register(Vocab.operationUploadABAC, ct, loaderLabelsProc));
    }

    @Override
    public String name() {
        return "RDF ABAC";
    }

    @Override
    public void prepare(FusekiServer.Builder serverBuilder, Set<String> datasetNames, Model configModel) {
        for ( String name : datasetNames ) {
            prepare1(serverBuilder, name, configModel);
        }
    }

    // Inspect the configuration.
    private void prepare1(Builder serverBuilder, String name, Model configModel) {
        if ( configModel == null )
            return;

        DatasetGraph dsg = serverBuilder.getDataset(name);
        if ( dsg == null )
            return;
        if ( dsg instanceof DatasetGraphABAC ) {
            DatasetGraphABAC dsgz = (DatasetGraphABAC)dsg;
            FmtLog.info(LOG, "ABAC Dataset: %s", name);
            FmtLog.info(LOG, "  Default label: %s", dsgz.getDefaultLabel());
            FmtLog.info(LOG, "  Access attr  : %s", dsgz.getAccessAttributes());
        }
    }

    @Override
    public void configDataAccessPoint(DataAccessPoint dap, Model configModel) {
        DatasetGraph dsg = dap.getDataService().getDataset();
        if ( ! ( dsg instanceof DatasetGraphABAC ) ) {
            // Not ABAC.
            return;
        }
        // Replace the standard query handler and upload functions for this DataService
        ActionProcessor procQuery = new ABAC_SPARQL_QueryDataset(getUser);
        ActionProcessor procGSPR = new ABAC_GSP_R(getUser);
        ActionProcessor procUpload = new ABAC_ChangeDispatch();

        // Replace standard processors with ABAC ones.
        replace(dap, Operation.Query,  procQuery);
        replace(dap, Operation.GSP_R,  procGSPR);
        replace(dap, Operation.Upload, procUpload);
    }

    private void replace(DataAccessPoint dap, Operation operation, ActionProcessor proc) {
        DataService dataService = dap.getDataService();
        dataService.getEndpoints(operation).forEach(ep->{
//            Endpoint ep2 = Endpoint.create()
//                    .operation(ep.getOperation())
//                    .processor(proc)
//                    .endpointName(ep.getName())
//                    .build();
            // Mutates the endpoint.
            ep.setProcessor(proc);
        });
    }

    // Reverse lookup: find the resource for the registry entry name in the configuration file.
    private Resource findDatasetResource(String name, Model configModel) {
        StmtIterator sIter = configModel.listStatements(null, FusekiVocab.pServiceName, ResourceFactory.createPlainLiteral(name));

        // If not found try by canonical name, try again non-canonical (or SPARQL query)
        if ( ! sIter.hasNext() && DataAccessPoint.isCanonical(name)) {
            String name2 = name.substring(1);
            sIter = configModel.listStatements(null, FusekiVocab.pServiceName, ResourceFactory.createPlainLiteral(name2));
        }
        try {
            if ( sIter.hasNext() ) {
                Statement s = sIter.next();
                if ( sIter.hasNext() )
                    throw new FusekiException("Multiple endpoints for name '+name+'");
                Resource subject = s.getSubject();
                return subject;
            }
            throw new FusekiException("Can't find the dataset in the configuration: '"+name+"'") ;
        } finally { sIter.close(); }
    }

    /** Check the server */
    @Override
    public void server(FusekiServer server) {
        server.getDataAccessPointRegistry().forEach((name,dap) -> {
            DatasetGraph dsg = dap.getDataService().getDataset();
             if ( dsg instanceof DatasetGraphABAC )
                 checkDataset(dap);
        });
    }

    // Check ActionProcessors are authorization ones.
    private void checkDataset(DataAccessPoint dap) {
        Collection<Endpoint> endpoints = dap.getDataService().getEndpoints();
        endpoints.forEach(ep->{
            ActionProcessor proc = ep.getProcessor();
            if ( proc != null && ! isAuthzProcessor(proc) ) {
                FmtLog.warn(Fuseki.configLog,
                             "%s: Non-authorization operation processor on DatasetGraphAuthz: class %s",
                             dap.getName(), proc.getClass().getSimpleName());
            }
        });
    }

    private boolean isAuthzProcessor(ActionProcessor proc) {
        return (proc instanceof ABAC_Processor);
    }
}