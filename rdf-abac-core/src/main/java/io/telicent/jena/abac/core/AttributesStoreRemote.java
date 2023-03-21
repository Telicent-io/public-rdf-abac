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

import static org.apache.jena.http.HttpLib.execute;
import static org.apache.jena.http.HttpLib.toRequestURI;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import io.telicent.jena.abac.attributes.AttributeValue;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.atlas.lib.StreamOps;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.http.HttpLib;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributesStoreRemote implements AttributesStore {

    private final Logger LOG = LoggerFactory.getLogger(AttributesStoreRemote.class);

    // [ABAC]
    // Awaiting update to remote user attribute store.
    // Will need a cache of user attributes
    // Will need a cache of hierarchies

    private final String userTemplate = "{user}";
    private final String lookupUserEndpoint;
    private final String hierarchyTemplate = "{name}";
    private final String lookupHierarchyEndpoint;
    /**
     * {@code endpoint} is the URL root of the attribute store.
     * It should include {user} which is substituted before use.
     */
    public AttributesStoreRemote(String lookupUserEndpoint, String lookupHierarchyEndpoint) {
        if ( lookupHierarchyEndpoint == null )
            LOG.info("No hierarchy lookup service configured");

        this.lookupUserEndpoint = lookupUserEndpoint;
        this.lookupHierarchyEndpoint = lookupHierarchyEndpoint;
        if ( ! lookupUserEndpoint.contains(userTemplate) )
            LOG.warn("Endpoint does not contain `"+userTemplate+"`: "+lookupUserEndpoint);
        if ( lookupHierarchyEndpoint != null && ! lookupHierarchyEndpoint.contains(hierarchyTemplate) )
            LOG.warn("Endpoint does not contain `"+hierarchyTemplate+"`: "+lookupHierarchyEndpoint);
    }

    /*
     * Get attributes from this {@code AttributeStore} for a given user.
     * The user must not be null.
     * <ul>
     * <li>null {@literal ->} unknown user.
     * <li>empty AttributeSet {@literal ->} known user, no attributes.
     */
    public static String jAttributes = "attributes";

    // JsonString to string, with checking.
    private String jsonStringToString(JsonValue jvStr, JsonValue source) {
        if ( ! jvStr.isString() ) {
            LOG.error("\"attributes\" element not a string: : "+JSON.toStringFlat(jvStr)+" in "+JSON.toStringFlat(source));
            return null;
        }
        return jvStr.getAsString().value();
    };

    @Override
    public AttributeValueSet attributes(String userName) {
        String requestURL = A.substitute(lookupUserEndpoint, userTemplate, userName);
        FmtLog.info(LOG, "User attribute request: %s", requestURL);
        try {
            // Send: "/users/lookup/{user}"
            java.net.http.HttpClient httpClient = HttpEnv.getDftHttpClient();
            HttpRequest.Builder builder = HttpLib.requestBuilderFor(requestURL).uri(toRequestURI(requestURL)).GET();
            builder.setHeader(HttpNames.hAccept, WebContent.contentTypeJSON);
            HttpRequest request = builder.build();

            // Response
            //   { "attributes" : [ string1, string2 , ... ] }
            // each string is a attribute-value pair (with any necessary quoting within the string).
            //   See project https://github.com/Telicent-io/rdf-abac/tree/main/docs/abac-specification.md
            //   See ?? for the network API.
            HttpResponse<InputStream> response = execute(httpClient, request);
            InputStream in = response.body();
            if ( response.statusCode() == 404 ) {
                String x = IO.readWholeFileAsUTF8(in);
                FmtLog.warn(LOG, "Response from remote attribute store : 404\n%s", x);
                return null;
            }

            JsonValue jv = JSON.parseAny(in);
            if ( ! jv.isObject() ) {
                LOG.error("Response from remote attribute store is not a JSON object: "+JSON.toStringFlat(jv));
                return null;
            }

            if ( ! jv.isObject() ) {
                LOG.error("Response from remote attribute store is not a JSON object: "+JSON.toStringFlat(jv));
                return null;
            }

            JsonValue jva = jv.getAsObject().get(jAttributes);


            if ( jva == null ) {
                LOG.error("Response from remote attribute store does not contain \""+jAttributes+"\" field: "+JSON.toStringFlat(jv));
                return null;
            }

            if ( ! jva.isArray() ) {
                LOG.error("\""+jAttributes+"\" is not a JSON array: "+JSON.toStringFlat(jva));
                return null;
            }

            FmtLog.info(LOG,  "Received (%s): %s", userName, JSON.toStringFlat(jva));

            // Expected: JSON:
            //     { ...
            //       attributes: [ "attribute-value", "attribute-value", "attribute-value"],
            //       ...
            //     }

            List<String> s = jva.getAsArray().stream()
                    .map(a->jsonStringToString(a, jva))
                    .filter(Objects::nonNull)
                    .toList();

            try {
                AttributeValueSet attrValueSet =  parseResponse.apply(s.stream());
                FmtLog.info(LOG,  "Response %s", attrValueSet);
                return attrValueSet;
            } catch (AttributeSyntaxError ex) {
                FmtLog.info(LOG, "AttributeSyntaxError in response: %s. Response = |%s|", ex.getMessage(), JSON.toStringFlat(jva));
                throw ex;
            }
        }
        catch (HttpException ex) {
            LOG.error("HttpException", ex);
            return null;
        }
    }

    private static Function<Stream<String>, AttributeValueSet> parseResponse = (Stream<String> items) -> {
        Stream<AttributeValue> s2 = items.map((str) -> AE.parseAttrValue(str));
        List<AttributeValue> attrValues = StreamOps.toList(s2);
        AttributeValueSet attributeValueSet = AttributeValueSet.of(attrValues);
        return AttributeValueSet.of(attrValues);
    };

    @Override
    public boolean hasHierarchy(Attribute attribute) {
        Hierarchy hierarchy = getHierarchy(attribute);
        return hierarchy != null && ! hierarchy.values().isEmpty();
    }

    public static String jHierarchyLevels1 = "tiers";
    public static String jHierarchyLevels2 = "levels";

    @Override
    public Hierarchy getHierarchy(Attribute attribute) {
        if ( lookupHierarchyEndpoint == null )
            return null;

        String requestURL = A.substitute(lookupHierarchyEndpoint, hierarchyTemplate, attribute.name());
        LOG.info("Hierarchy lookup request: "+requestURL);
        try {
            java.net.http.HttpClient httpClient = HttpEnv.getDftHttpClient();
            HttpRequest.Builder builder = HttpLib.requestBuilderFor(requestURL).uri(toRequestURI(requestURL)).GET();
            builder.setHeader(HttpNames.hAccept, WebContent.contentTypeJSON);
            HttpRequest request = builder.build();
            HttpResponse<InputStream> response = execute(httpClient, request);
            InputStream in = response.body();
            if ( response.statusCode() == 404 ) {
                String x = IO.readWholeFileAsUTF8(in);
                LOG.warn("Response from remote attribute store : 404\n"+x);
                return null;
            }

            JsonValue jv = JSON.parseAny(in);
            if ( ! jv.isObject() ) {
                LOG.error("Response from remote attribute store is not a JSON object: "+JSON.toStringFlat(jv));
                return null;
            }

            // Response
            // { "uuid": "...." ,
            //   "name": "...." ,
            //   "tiers" : [ string , string, ]"
            // }
            //

            if ( ! jv.isObject() ) {
                LOG.error("Response from remote attribute store is not a JSON object: "+JSON.toStringFlat(jv));
                return null;
            }
            JsonValue jva = getFromJsonObject(jv.getAsObject(), jHierarchyLevels1, jHierarchyLevels2);
            if ( jva == null ) {
                LOG.info("Response: no such hierarchy: "+attribute.name());
                return null;
            }

            if ( ! jva.isArray() ) {
                LOG.error("\""+jHierarchyLevels1+"\" is not a JSON array: "+JSON.toStringFlat(jva));
                return null;
            }

            List<String> levels = jva.getAsArray().stream()
                    .map(a->jsonStringToString(a, jva))
                    .filter(Objects::nonNull)
                    .toList();

            Hierarchy hierarchy = Hierarchy.create(attribute, levels);
            LOG.info("Response: "+hierarchy);
            return hierarchy;
        }
        catch (HttpException ex) {
            LOG.error("HttpException", ex);
            return null;
        }
    }

    /** Get a JSON value from a field with different possible names */
    static JsonValue getFromJsonObject(JsonObject jObj, String...fieldNames) {
        for ( String name : fieldNames ) {
            JsonValue jv = jObj.get(name);
            if( jv != null )
                return jv;
        }
        return null;
    }

    @Override
    public Set<String> users() {
        return Set.of();
    }
}
