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

import java.util.Objects;
import java.util.function.Function;

import org.apache.jena.fuseki.servlets.GSP_R;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.sparql.core.DatasetGraph;

public class ABAC_GSP_R extends GSP_R implements ABAC_Processor {

    private final Function<HttpAction, String> getUser;

    public ABAC_GSP_R(Function<HttpAction, String> getUser) {
        this.getUser = Objects.requireNonNull(getUser, "getUser is null");
    }

    @Override
    protected DatasetGraph decideDataset(HttpAction action) {
        DatasetGraph dsg0 =action.getActiveDSG();
        DatasetGraph dsg = ABAC_Request.decideDataset(action, dsg0, getUser);
        return dsg;
    }
}
