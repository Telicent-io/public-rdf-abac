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

package io.telicent.jena.abac.assembler;

import static io.telicent.jena.abac.core.VocabAuthzDataset.pDataset;

import io.telicent.jena.abac.core.DatasetGraphABAC;
import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.Mode;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.assembler.DatasetAssembler;

/**
 * A secured dataset.
 */
public class SecuredDatasetAssembler extends DatasetAssembler implements Assembler {

    public SecuredDatasetAssembler() {}

    @Override
    public Dataset open(Assembler a, Resource root, Mode mode) {
        DatasetGraph dsg = createDataset(a, root);
        return DatasetFactory.wrap(dsg);
    }

    @Override
    public DatasetGraph createDataset(Assembler a, Resource root) {
        /*
         *   [] rdf:type authz:DatasetAuthz ;
         *       authz:labels  <store of labels>      -- default: in-memory store
         *       authz:accessAttributes "" ;          -- default: none
         *       authz:tripleDefaultAttributes "" ;   -- default: null (sstem default applies)
         *
         *       authz:attributes "filename" ;        -- User attributes and hierarchies
         *       ## OR
         *       ## authz:attributesURL "URL" ;
         *
         *       ## Underlying datasets.
         *       authz:dataset <base data> ;          -- Data
         *       .
         */
        DatasetGraph dsgBase = createBaseDataset(root, pDataset);
        DatasetGraphABAC dsgAuthz = Secured.buildDatasetGraphAuthz(dsgBase, root);
        if ( dsgAuthz == null )
            throw new AssemblerException(root, "Failed to find configuration for a DatasetGraphAuthz");
        return dsgAuthz;
    }
}

