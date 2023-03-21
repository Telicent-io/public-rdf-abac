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

package io.telicent.jena.abac.core;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 * General vocabulary for the ABAC security.
 * This is not the {@link VocabAuthzDataset assembler vocabulary}.
 * This includes the multi-file packing of data-labels-rules into a dataset/TriG file.
 */
public class VocabAuthz {
    private static String NS = "http://telicent.io/security#" ;
    public static String getURI() { return NS; }

    public static Node graphForLabels = NodeFactory.createURI(NS+"labels");
}
