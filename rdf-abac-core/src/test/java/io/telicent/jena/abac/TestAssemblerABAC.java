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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.VocabAuthzDataset;
import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.core.assembler.AssemblerUtils;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.Test;

/**
 * Assembler testing.
 */
public class TestAssemblerABAC {
    static { JenaSystem.init(); }

    private static String DIR = "src/test/files/dataset/";

    @Test public void assemble1() {
        JenaSystem.init();
        VocabAuthzDataset.init();
        // Directly make
        Dataset ds = (Dataset)AssemblerUtils.build(DIR+"abac-assembler.ttl", VocabAuthzDataset.tDatasetAuthz);
        assertNotNull(ds);
        DatasetGraphABAC dsgz = (DatasetGraphABAC)ds.asDatasetGraph();
        assertNotNull(dsgz.labelsStore());
    }
}
