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

import static org.junit.jupiter.api.Assertions.*;

import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import io.telicent.jena.abac.attributes.syntax.tokens.Token;
import io.telicent.jena.abac.attributes.syntax.tokens.TokenType;
import io.telicent.jena.abac.attributes.syntax.tokens.Tokenizer;
import io.telicent.jena.abac.attributes.syntax.tokens.TokenizerABAC;
import org.apache.jena.atlas.lib.Lib;
import org.junit.jupiter.api.Test;

/** Parser test - the test is whether the input is legal */
public class TestAttributeParser extends AbstractParserTests {

    @Test public void parse_token_01()  { parseToken("ABC", TokenType.WORD); }
    @Test public void parse_token_02()  { parseToken(" | ", TokenType.VBAR); }
    @Test public void parse_token_03()  { parseToken("||",  TokenType.LOGICAL_OR); }
    @Test public void parse_token_04()  { parseToken("&",   TokenType.AMPERSAND); }
    @Test public void parse_token_05()  { parseToken("&&",  TokenType.LOGICAL_AND); }

    @Test public void parse_token_06()  { parseToken("(  ", TokenType.LPAREN); }
    @Test public void parse_token_07()  { parseToken(" ) ", TokenType.RPAREN); }

    @Test public void parse_token_08()  { parseToken(" { ", TokenType.LBRACE); }
    @Test public void parse_token_09()  { parseToken(" } ", TokenType.RBRACE); }

    @Test public void parse_token_10()  { parseToken("!",   TokenType.EMARK); }
    @Test public void parse_token_11()  { parseToken("*",   TokenType.STAR); }

    @Test public void parse_token_12()  { parseToken(">",   TokenType.GT); }
    @Test public void parse_token_13()  { parseToken(">=",  TokenType.GE); }
    @Test public void parse_token_14()  { parseToken("<",   TokenType.LT); }
    @Test public void parse_token_15()  { parseToken("<=",  TokenType.LE); }
    @Test public void parse_token_16()  { parseToken("=",   TokenType.EQ); }
    @Test public void parse_token_17()  { parseToken("==",  TokenType.EQUIVALENT); }
    @Test public void parse_token_18()  { parseToken("!=",  TokenType.NE); }

    @Test public void parse_word_01()   { parseWord("a"); }
    @Test public void parse_word_02()   { parseWord("aa"); }
    @Test public void parse_word_03()   { parseWord("a1"); }
    @Test public void parse_word_04()   { parseWord("a2z"); }

    @Test public void parse_word_05()   { parseWord("a.", "a"); }
    @Test public void parse_word_06()   { parseWord("_"); }
    @Test public void parse_word_07()   { parseWord("__"); }
    @Test public void parse_word_08()   { parseWord("_z"); }

    @Test public void parse_word_10()   { parseWord("a_:+-a"); }
    @Test public void parse_word_11()   { parseWord("abc_"); }
    @Test public void parse_word_12()   { parseWord("a_:+-_"); }
    @Test public void parse_word_13()   { parseWord("  a   ", "a"); }
    @Test public void parse_word_14()   { parseWord("a:", "a"); }

    // Not valid.
    @Test public void parse_word_15()   { parseWord("a::", "a"); }

    // Bad/truncate
    @Test public void parse_word_21()   { parseWord("a+", "a"); }
    @Test public void parse_word_22()   { parseWord("a-", "a"); }
    @Test public void parse_word_23()   { parseWord("a.", "a"); }
    @Test public void parse_word_24()   { parseWord("a_", "a_"); }
    @Test public void parse_word_25()   { parseWord("a+b"); }
    @Test public void parse_word_26()   { parseWord("a+","a"); }

    @Test public void parse_word_34()   { parseWord("'abc def'", "abc def"); }
    @Test public void parse_word_35()   { parseWord("\"abc def\"", "abc def"); }
    @Test public void parse_word_36()   { parseWord("'abc\\t'", "abc\t"); }

    @Test public void parse_word_37()   { parseWord("'\\u0041'", "A"); }
    @Test public void parse_word_38()   { parseWord("'\\U00000041'", "A"); }

    @Test public void parse_number_01() { parseNumber("57"); }
    @Test public void parse_number_02() { parseNumber("+57"); }
    @Test public void parse_number_03() { parseNumber("-57e0"); }
    @Test public void parse_number_04() { parseNumber("-57e-0"); }
    @Test public void parse_number_05() { parseNumber("0xABCD"); }
    @Test public void parse_number_06() { parseNumber("12.23"); }
    @Test public void parse_number_07() { parseNumber("12.23E+10"); }

    @Test public void parse_bad_token_01()  { parseBadToken("0x"); }
    @Test public void parse_bad_token_02()  { parseBadToken("0x"); }
    @Test public void parse_bad_token_03()  { parseBadToken("0xx"); }

    // Legal word. Fails in the parser, not the tokenizer.
    @Test public void parse_keyword_01()    { parseWord("true"); }
    @Test public void parse_keyword_02()    { parseWord("false"); }

    // Not handled by the tokenizer
    @Test public void parse_bad_word_01()   { parseBadWord("*"); }
    @Test public void parse_bad_word_02()   { parseBadWord("!"); }

    @Test public void parse_bad_word_03()   { parseBadWord("a/b"); }
    @Test public void parse_bad_word_04()   { parseBadWord("a-"); }
    @Test public void parse_bad_word_05()   { parseBadWord("+"); }
    @Test public void parse_bad_word_06()   { parseBadWord("a+"); }

    @Test public void parse_bad_word_07()   { parseBadWord("+"); }
    @Test public void parse_bad_word_08()   { parseBadWord("-"); }

    @Test public void parse_bad_word_09()   { parseBadWord("\\u0020"); }
    @Test public void parse_bad_word_10()   { parseBadWord(".a"); }

    @Test public void parse_bad_word_11()   { parseBadWord(":a:a"); }
    @Test public void parse_bad_word_12()   { parseBadWord(":"); }
    @Test public void parse_bad_word_13()   { parseBadWord("::"); }
    @Test public void parse_bad_word_14()   { parseBadWord("."); }

    @Test public void parse_bad_word_20()   { parseBadWord("-a"); }
    @Test public void parse_bad_word_21()   { parseBadWord("+a"); }

    @Test public void parse_bad_word_99()   { parseBadWord(""); }

    private void parseToken(String input, TokenType expected) {
        Tokenizer tokens = TokenizerABAC.fromString(input);
        assertTrue(tokens.hasNext(), "Token expected, none found");
        Token tok = tokens.next();
        assertNotNull(tok);
        assertFalse(tokens.hasNext());
        if ( expected != null )
            assertEquals(expected, tok.getType());
    }

    private void parseWord(String input) {
        parseWord(input, input);
    }

    private void parseWord(String input, String expected) {
        Tokenizer tokens = TokenizerABAC.fromString(input);
        assertTrue(tokens.hasNext());
        Token t = tokens.next();
        assertTrue(t.isWord()||t.isString(), t.toString());
        assertNotNull(t.getImage());
        assertEquals(expected, t.asWord());
        if ( input.equals(expected) )
            assertFalse(tokens.hasNext(), "Not the end of tokens");
    }

    private void parseNumber(String input) {
        Tokenizer tokens = TokenizerABAC.fromString(input);
        Token t = tokens.next();
        assertTrue(t.isNumber(), "Token is "+t);
        assertNotNull(t.getImage());
        assertFalse(tokens.hasNext());
    }

    private void parseBadWord(String input) {
        try {
            Tokenizer tokens = TokenizerABAC.fromString(input);
            if ( ! tokens.hasNext() )
                return;
            Token t = tokens.next();
            assertTrue( !t.isWord() || Lib.notEqual(input,t.getImage()), ""+t);
        } catch (AttributeSyntaxError ex) {}
    }

    private void parseBadToken(String input) {
        try {
            Tokenizer tokens = TokenizerABAC.fromString(input);
            Token t = tokens.next();
            assertTrue( !t.isWord() || Lib.notEqual(input,t.getImage()), ""+t);
        } catch (AttributeSyntaxError ex) {}
    }
}
