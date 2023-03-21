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

import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import org.junit.jupiter.api.Test;

public class TestAttributeExprParse extends AbstractParserTests {

    @Test public void parse_expression_01()  { parseExpression("attribute"); }
    @Test public void parse_expression_02()  { parseExpression("attribute = value"); }

    @Test public void parse_expression_03()  { parseExpression("a=k1 & b=k2"); }
    @Test public void parse_expression_04()  { parseExpression("a=k1 | b=k2"); }
    @Test public void parse_expression_05()  { parseExpression("a"); }

//    @Test public void parse_expression_06()  { parseExpression("a > b"); }
//    @Test public void parse_expression_07()  { parseExpression("a >= b"); }
//    @Test public void parse_expression_08()  { parseExpression("a < b"); }
//    @Test public void parse_expression_09()  { parseExpression("a <= b"); }

    @Test public void parse_expression_disabled_06()  { parseBadExpression("a > b"); }
    @Test public void parse_expression_disabled_07()  { parseBadExpression("a >= b"); }
    @Test public void parse_expression_disabled_08()  { parseBadExpression("a < b"); }
    @Test public void parse_expression_disabled_09()  { parseBadExpression("a <= b"); }

    @Test public void parse_expression_10()  { parseExpression("a != b"); }

    // Keywords as values.
    @Test public void parse_expression_11()  { parseExpression("a = true"); }
    @Test public void parse_expression_12()  { parseExpression("a=false"); }

    @Test public void parse_expression_20() { parseExpression("'my attr'"); }
    @Test public void parse_expression_21() { parseExpression("\"my attr\""); }
    @Test public void parse_expression_22() { parseExpression("'my attr' != \"some value\""); }

    @Test public void parse_bad_expression_01()  { parseBadExpression("a >"); }
    @Test public void parse_bad_expression_02()  { parseBadExpression("< a"); }
    @Test public void parse_bad_expression_03()  { parseBadExpression("<a"); }
    @Test public void parse_bad_expression_04()  { parseBadExpression("a ! b"); }
    @Test public void parse_bad_expression_05()  { parseBadExpression("! a"); }
    @Test public void parse_bad_expression_06()  { parseBadExpression("!= b"); }

    // Keywords can't be attributes.
    @Test public void parse_bad_expression_10()  { parseBadExpression("true = a"); }
    @Test public void parse_bad_expression_11()  { parseBadExpression("false = a"); }


    private void parseExpression(String str) {
        AttributeExpr attrExpr = AE.parseExpr(str);
        assertNotNull(attrExpr);
    }

    private void parseBadExpression(String str) {
        try {
            AttributeExpr attrExpr = AE.parseExpr(str);
            fail("Parsed bad expression: "+str);
        } catch (AttributeSyntaxError ex) {}
    }
}
