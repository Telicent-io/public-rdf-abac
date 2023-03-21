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

package io.telicent.jena.abac;

import io.telicent.jena.abac.assembler.SecuredDatasetAssembler;
import io.telicent.jena.abac.attributes.syntax.AEX;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.LabelsGetter;
import org.apache.jena.util.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysABAC {
    /** Constant for "deny all" */
    public static final String denyLabel = AEX.strDENY;

    /** Constant for "allow all" */
    public static final String allowLabel = AEX.strALLOW;

    /**
     * System-wide default used when there isn't an appropriate label or an error occurred.
     * <p>
     * Normally, a default attribute is associated with {@link DatasetGraphABAC}
     * via the {@link SecuredDatasetAssembler} configuration.
     */
    public static final String systemDefaultTripleAttributes = denyLabel;

    /**
     * Result if there are no labels or label patterns configured for a dataset.
     * ({@link LabelsGetter} returns null).
     * @implNote
     * Used by {@code SecurityFilterByLabel}.
     */
    public static final boolean DefaultChoiceNoLabels = true;

//    /**
//     * If there are labels configured for a dataset (including having an empty label store)
//     * and none found for the triple being checked ({@link LabelsGetter} returned "").
//     * @implNote
//     * Used by {@code SecurityFilterByLabel}.
//     */
//    public static final boolean DefaultChoiceNoAttributes = true;

    public static Logger SYSTEM_LOG = LoggerFactory.getLogger("io.telicent.jena.abac");

    public static final String PATH         = "io.telicent.jena.abac";
    static private String metadataLocation  = "io/telicent/jena/abac/rdf-abac.xml";
    static private Metadata metadata        = new Metadata(metadataLocation);

    /** The product name */
    public static final String NAME         = "RDF ABAC";

    /** The full name of the current ARQ version */
    public static final String VERSION      = metadata.get(PATH+".version", "unknown");

    /** The date and time at which this release was built */
    public static final String BUILD_DATE   = metadata.get(PATH+".build.datetime", "unset");

    public static void init() {
        // FMod_ABAC logs.
        //FmtLog.info(SYSTEM_LOG, "RDF ABAC %s", SysABAC.VERSION);
    }
}
