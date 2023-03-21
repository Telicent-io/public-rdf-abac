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

import java.util.Objects;

import org.apache.jena.atlas.lib.IRILib;

/**
 * Internal library and contants for rdf-abac.
 */
public class A {

    /**
     * URL template substitution.
     */
    public static String substitute(String template, String parameterName, String value) {
        Objects.requireNonNull(template);
        Objects.requireNonNull(parameterName);
        Objects.requireNonNull(value);

        String safeValue = IRILib.encodeUriPath(value);
        if ( ! template.contains(parameterName) )
            throw new AuthzException(String.format("Parameter %s not found in %s", template, parameterName));

        String requestURL = template.replace(parameterName, safeValue);
        return requestURL;
    }
}
