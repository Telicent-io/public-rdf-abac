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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.lib.ListUtils;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphZero;
import org.junit.jupiter.api.Test;

/**
 * List of expressions.
 */
public class TestAttributeExprList extends AbstractParserTests {

    private static final DatasetGraph dsg = DatasetGraphZero.create();

    @Test public void attribute_list_00() { aList(""); }
    @Test public void attribute_list_01() { aList(" "); }
    @Test public void attribute_list_02() { aList("a1", "a1"); }
    @Test public void attribute_list_03() { aList("*", "*"); }
    @Test public void attribute_list_04() { aList("!", "!"); }

    @Test public void attribute_list_10() { aList("a=1, zz", "a = 1", "zz"); }
    @Test public void attribute_list_11() { aList("   a1 ,  z ", "a1", "z"); }

    @Test public void attribute_list_disabled_01() { notAList("a1, a2<3"); }

    @Test public void attribute_bad_list_01() { notAList("a,"); }
    @Test public void attribute_bad_list_02() { notAList(",a"); }
    @Test public void attribute_bad_list_03() { notAList("a+,b"); }
    @Test public void attribute_bad_list_04() { notAList("a,+b"); }

    // Not in attribute lists
    @Test public void attribute_bad_list_10() { notAList("a,!,b"); }
    @Test public void attribute_bad_list_11() { notAList("k=v,*"); }

    private void aList(String str, String...expected) {
        List<AttributeExpr> attrs = AE.parseExprList(str);
        List<AttributeExpr> expectedObj = Iter.of(expected).map(AE::parseExpr).toList();
        assertTrue(ListUtils.equalsUnordered(attrs, expectedObj));
    }

    private void notAList(String str) {
        try {
            List<AttributeExpr> attrs = AE.parseExprList(str);
            fail("Parsed '"+str+"'");
        } catch (AttributeSyntaxError ex) {}
    }
}
