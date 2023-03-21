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

import static io.telicent.jena.abac.attributes.ValueTerm.FALSE;
import static io.telicent.jena.abac.attributes.ValueTerm.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphZero;
import org.junit.jupiter.api.Test;

/*
 * Test evaluation of attribute labels, give request attributes and any hierarchies.
 */

public class TestAttributeExprEval extends AbstractParserTests {
    private static final DatasetGraph dsg = DatasetGraphZero.create();

    // Simple attributes
    @Test public void eval_term_01() { test("a1", TRUE); }

    @Test public void eval_term_02() { test("a9", FALSE); }

    @Test public void eval_term_03() { test("*", TRUE); }        // Allow all

    @Test public void eval_term_04() { test("!", FALSE); }       // Deny all.

    // Expressions
    @Test public void eval_expr_01() { test("a1|a9", TRUE); }

    @Test public void eval_expr_02() { test("a1&a9", FALSE); }

    @Test public void eval_expr_03() { test("a1&(a2|a9)", TRUE); }

    @Test public void eval_expr_04() { test("(a1&a8)|(a2&a9)", FALSE); }

    @Test public void eval_expr_05() { test("( a1 & a2 ) | ( a8 & a9 )", TRUE); }

    // General, with hierarchies
    static Hierarchy hierarchy = Hierarchy.create("attrH", "public", "restricted", "secret", "private");
    static Map<Attribute, Hierarchy> map = Map.of(hierarchy.attribute(), hierarchy);

    @Test public void eval_01() { test("attr1=1", "attr1=1", TRUE); }
    @Test public void eval_02() { test("attr1=1", "attr1", FALSE); }
    @Test public void eval_03() { test("attr1=true", "attr1", TRUE); }
    @Test public void eval_04() { test("attr1", "attr1=true", TRUE); }

    @Test public void eval_05() { test("attr1=2", "attr1=1", FALSE); }
    @Test public void eval_06() { test("attr2=1", "attr1=1", FALSE); }

    @Test public void eval_07() { test("attrH=restricted", "attrH=public", FALSE); }
    @Test public void eval_08() { test("attrH=restricted", "attrH=restricted", TRUE); }
    @Test public void eval_09() { test("attrH=restricted", "attrH=secret", TRUE); }
    @Test public void eval_10() { test("attrH=restricted", "attrH=private", TRUE); }

    // Repeat attribute values in request
    @Test public void eval_11() { test("attrH=restricted", "attrH=public, attrH=private", TRUE); }
    @Test public void eval_12() { test("attrH=restricted", "attrH=private, attrH=public", TRUE); }
    @Test public void eval_13() { test("attrH=restricted", "attr1=00, attrH=secret, attrH=2", TRUE); }

    @Test public void eval_20() { test("attrH=restricted & attr1=2", "attrH=private", FALSE); }
    @Test public void eval_21() { test("attrH=restricted && attr1=2", "attrH=private", FALSE); }

    @Test public void eval_22() { test("attrH=restricted | attr1=2", "attrH=private", TRUE); }
    @Test public void eval_23() { test("attrH=restricted || attr1=2", "attrH=private", TRUE); }

    // Test of 0.3, 0.4 syntax used for expanded hierarchies. Do not use "=".
    @Test public void eval_compat_01() { test("a:x1", "a:x1", TRUE); }

    @Test public void eval_bad_01() { bad(AttributeSyntaxError.class, "attr1=A || *", "attr1=A"); }
    @Test public void eval_bad_02() { bad(AttributeSyntaxError.class, "* || attr1=A", "attr1=A"); }
    @Test public void eval_bad_03() { bad(AttributeSyntaxError.class, "attr1=A || !", "attr1=A"); }
    @Test public void eval_bad_04() { bad(AttributeSyntaxError.class, "! || attr1=A", "attr1=A"); }


    private <X extends Throwable> void bad(Class<X> expectedExceptionClass, String attributeExpr, String attributeValues) {
        assertThrows(expectedExceptionClass, ()->test(attributeExpr, attributeValues, null));
    }

    private void test(String str, ValueTerm expected) {
        test(str, "a1, a2", expected);
    }

    private void test(String attributeExpr, String attributeValues, ValueTerm expected) {
        ValueTerm vTerm = AE.eval(attributeExpr, attributeValues, map::get);
        assertEquals(expected, vTerm);
        if (expected == null )
            fail("Did not expect to eval");
    }
}
