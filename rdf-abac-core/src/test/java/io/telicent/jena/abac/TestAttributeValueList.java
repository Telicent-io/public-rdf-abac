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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import io.telicent.jena.abac.attributes.AttributeValue;
import org.junit.jupiter.api.Test;

public class TestAttributeValueList extends AbstractParserTests {

    @Test public void parse_attrValueList_01() { parseAttrValueList("k=v"); }
    @Test public void parse_attrValueList_02() { parseAttrValueList("a"); }
    @Test public void parse_attrValueList_03() { parseAttrValueList("a,b,c"); }
    @Test public void parse_attrValueList_04() { parseAttrValueList("k1=v1 , k2=v2"); }
    @Test public void parse_attrValueList_05() { parseAttrValueList("k1=x1 , k1=x2"); }

    @Test public void parse_attrValueList_10() { parseAttrValueList("'my attr' = 'some value'"); }

    @Test public void parse_bad_attrValueList_01() { parseBadAttrValueList("k=v,"); }
    @Test public void parse_bad_attrValueList_02() { parseBadAttrValueList(",k=v"); }
    @Test public void parse_bad_attrValueList_03() { parseBadAttrValueList("k<v"); }
    @Test public void parse_bad_attrValueList_04() { parseBadAttrValueList("  ,  "); }
    @Test public void parse_bad_attrValueList_05() { parseBadAttrValueList("k != v"); }

    @Test public void parse_bad_attrValueList_06() { parseBadAttrValueList("*"); }
    @Test public void parse_bad_attrValueList_07() { parseBadAttrValueList("!"); }

    @Test public void parse_bad_attrValueList_08() { parseBadAttrValueList("k=v,*"); }
    @Test public void parse_bad_attrValueList_09() { parseBadAttrValueList("!,k=v"); }


    private void parseAttrValueList(String str) {
        List<AttributeValue> attrValList = AE.parseAttrValueList(str);
        assertNotNull(attrValList);
    }

    private void parseBadAttrValueList(String str) {
        try {
            List<AttributeValue> attrValList = AE.parseAttrValueList(str);
            fail("Parsed bad attribute-value list: "+str);
        } catch (AttributeSyntaxError ex) {}
    }
}
