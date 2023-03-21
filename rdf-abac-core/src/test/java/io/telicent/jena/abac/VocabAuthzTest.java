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

import io.telicent.jena.abac.core.VocabAuthz;
import io.telicent.jena.abac.core.VocabAuthzDataset;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 * Vocabulary for the ABAC AIO test format.
 * This is not the {@link VocabAuthzDataset assembler vocabulary}.
 */
public class VocabAuthzTest {
    private static String NS = "test:" ;
    public static String getURI() { return NS; }

    public static Node graphForTestResult   = NodeFactory.createURI(NS+"result");
    public static Node graphForAttributes   = NodeFactory.createURI(NS+"attributes");

    public static Node graphForLabels = VocabAuthz.graphForLabels;
    public static Node graphForRules = NodeFactory.createURI(NS+"rules");

    public static Node pDatasetDefaultLabel = NodeFactory.createURI(NS+"datasetDefaultLabel");


}
