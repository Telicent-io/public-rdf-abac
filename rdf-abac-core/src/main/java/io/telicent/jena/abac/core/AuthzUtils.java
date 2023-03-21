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

import org.apache.jena.atlas.lib.EscapeStr;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.out.NodeFmtLib;

public class AuthzUtils {

    public static String str(String str) {
        if ( str == null )
            return "null";
        return '"'+EscapeStr.stringEsc(str)+'"';
    }

    public static String str(Node node) {
        if ( node == null )
            return "null";
        return NodeFmtLib.strTTL(node);
    }

}
