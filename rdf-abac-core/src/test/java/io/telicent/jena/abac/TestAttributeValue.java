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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import io.telicent.jena.abac.attributes.AttributeValue;
import org.junit.jupiter.api.Test;

public class TestAttributeValue extends AbstractParserTests {

    @Test public void parse_attrValue_01() { parseAttrValue("k=v"); }
    @Test public void parse_attrValue_02() { parseAttrValue("a"); }

    @Test public void parse_attrValue_03() { parseAttrValue(" k =   v         "); }
    @Test public void parse_attrValue_04() { parseAttrValue("  a  "); }

    @Test public void parse_attrValue_05() { parseAttrValue("'my attr' = 'some value'"); }


    @Test public void parse_bad_attrValue_01() { parseBadAttrValue("k="); }
    @Test public void parse_bad_attrValue_02() { parseBadAttrValue("=v"); }
    @Test public void parse_bad_attrValue_03() { parseBadAttrValue("k<v"); }
    @Test public void parse_bad_attrValue_04() { parseBadAttrValue("k k<v"); }
    @Test public void parse_bad_attrValue_05() { parseBadAttrValue("k < v w"); }
    @Test public void parse_bad_attrValue_06() { parseBadAttrValue("*"); }
    @Test public void parse_bad_attrValue_07() { parseBadAttrValue("!"); }

    @Test public void parse_legacy_attrValue_01() { legacy(false, ()->parseBadAttrValue("user@host")); }
    @Test public void parse_no_legacy_attrValue_01() { legacy(true, ()->parseAttrValue("user@host")); }

    @Test public void parse_legacy_attrValue_02() { legacy(false, ()->parseBadAttrValue("first+last@host")); }
    @Test public void parse_no_legacy_attrValue_02() { legacy(true, ()->parseAttrValue("first+last@host")); }

    private void legacy(boolean setting, Runnable action) {
        boolean b = ABAC.LEGACY;
        ABAC.LEGACY = setting;
        try { action.run(); }
        finally { ABAC.LEGACY = b; }
    }

    private void parseAttrValue(String str) {
        AttributeValue attrVal = AE.parseAttrValue(str);
        assertNotNull(attrVal);
    }

    private void parseBadAttrValue(String str) {
        assertThrows("Parsed bad attribute-value: "+str, AttributeSyntaxError.class, ()->AE.parseAttrValue(str));
    }
}
