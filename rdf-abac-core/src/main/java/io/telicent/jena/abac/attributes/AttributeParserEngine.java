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

package io.telicent.jena.abac.attributes;

import static io.telicent.jena.abac.attributes.syntax.AEX.kwFALSE;
import static io.telicent.jena.abac.attributes.syntax.AEX.kwTRUE;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.syntax.*;
import io.telicent.jena.abac.attributes.syntax.tokens.Token;
import io.telicent.jena.abac.attributes.syntax.tokens.TokenType;
import io.telicent.jena.abac.attributes.syntax.tokens.Tokenizer;
import io.telicent.jena.abac.attributes.syntax.tokens.TokenizerABAC;

class AttributeParserEngine {

    // Expr = ExprOr | "*" | "!"
    // ExprOr = ExprAnd (| ExprOr)*
    // ExprAnd = ExprRel (& ExprAnd) *
    // ExprRel = Bracketted | Attr (RE ValueTerm)?
    // Attr = [a-zA-Z_](([a-zA-Z0-9_:.-+])*[a-zA-Z_0-9])?
    // ValueTerm = attr or signed number
    // RE = "=", "<", ">", "!="

    // Attr '=' ValueTerm


    private final Tokenizer tokenizer;

    AttributeParserEngine(String string) {
        this(TokenizerABAC.create().fromString(string).build());
    }

    AttributeParserEngine(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    Tokenizer tokenizer() {
        return tokenizer;
    }

    boolean endOfTokens() {
        return ! tokenizer.hasNext();
    }

    AttributeExpr attributeExpression( ) {
        AttributeExpr expr = readExprOr();
        return expr;
    }

    // Used in lists, not labels.
    AttributeValue attributeValue( ) {
        // Like readExprRel except only allowing "="
        if ( tokenizer.eof() )
            throw new AttributeSyntaxError("END");
        AE_Attribute attr = readAttribute();
        Attribute attribute = attr.attribute();

        if ( tokenizer.eof() )
            // Plain attribute
            return AttributeValue.of(attribute, AttributeValue.dftTrue);
        Token relToken = tokenizer.peek();
        if ( relToken.getType() != TokenType.EQ )
            // Plain attribute
            return AttributeValue.of(attribute, AttributeValue.dftTrue);

        // It's "=" EQ
        /*Token relToken = */tokenizer.next();
        if ( tokenizer.eof() )
            throw new AttributeSyntaxError("Expected value after `=`");

        AE_AttrValue expr2 = readAttributeValue();
        ValueTerm value = expr2.asValue();
        return AttributeValue.of(attribute, value);
    }

    // Hierarchy = Attribute ':' ( ValueTerm (',' ValueTerm)* )?

    Hierarchy hierarchy() {
        if ( tokenizer.eof() )
            throw new AttributeSyntaxError("END");
        AE_Attribute attr = readAttribute();
        Attribute attribute = attr.attribute();
        if ( tokenizer.eof() )
            throw new AttributeSyntaxError("Unexpected end to attribute value hierarchy");

        Token token = tokenizer.next();
        if ( ! token.hasType(TokenType.COLON) )
            throw new AttributeSyntaxError("Expected ':' after attribute name in hierarchy: "+token);
//            if ( tokenizer.eof() )
//                throw new AttributeSyntaxError("Unexpected end to attribute hierarchy");
        List<ValueTerm> values = parseListValues();
        return new Hierarchy(attribute, values);
    }

    List<ValueTerm> parseListValues() {
        Function<AttributeParserEngine, ValueTerm> parseOneItem = (parser) -> parser.readAttributeValue().asValue();
        List<ValueTerm> values = parseList(parseOneItem);
        return values;
    }

    private AttributeExpr readExprOr() {
        if ( tokenizer.eof() )
            throw new AttributeSyntaxError("END");
        AttributeExpr expr1 = readExprAnd();
        for(;;) {
            if ( tokenizer.eof() )
                return expr1;
            Token token = tokenizer.peek();
            // "|" or "||"
            if ( token.getType() == TokenType.VBAR || token.getType() == TokenType.LOGICAL_OR ) {
                tokenizer.next();
                AttributeExpr expr2 = readExprAnd();
                expr1 = new AE_Or(expr1, expr2);
                continue;
            }
            return expr1;
        }
    }

    private AttributeExpr readExprAnd() {
        if ( tokenizer.eof() )
            throw new AttributeSyntaxError("END");
        AttributeExpr expr1 = readExprUnary();
        for(;;) {
            if ( tokenizer.eof() )
                return expr1;
            Token token = tokenizer.peek();
            // "&" or "&&"
            if ( token.getType() == TokenType.AMPERSAND || token.getType() == TokenType.LOGICAL_AND  ) {
                Token t2 = tokenizer.next();
                AttributeExpr expr2 = readExprUnary();
                expr1 = new AE_And(expr1, expr2);
                continue;
            }
            return expr1;
        }
    }

    private AttributeExpr readExprUnary() {
        TokenType peek = tokenizer.peek().getType();

        if ( peek == TokenType.WORD || peek == TokenType.STRING )
            return readExprRel();

        if ( peek == TokenType.LPAREN ) {
            Token t = tokenizer.next();
            AttributeExpr expr = attributeExpression();
            if ( tokenizer.eof() )
                throw new AttributeSyntaxError("No RPAREN: "+t);
            Token token = tokenizer.next();
            if ( token.getType() == TokenType.RPAREN )
                return new AE_Bracketted(expr);
            throw new AttributeSyntaxError("Expected RPAREN: "+token);
        }

        if ( peek == TokenType.LBRACE ) {
            Token t1 = tokenizer.next();
            if ( tokenizer.eof() )
                throw new AttributeSyntaxError("No RBRACE: "+t1);
            Token token = tokenizer.next();
            if ( token.getType() != TokenType.WORD )
                throw new AttributeSyntaxError("Expected WORD after: "+t1);
            String varName = token.getImage();
            Token t2 = tokenizer.next();
            if ( t2.getType() == TokenType.RBRACE )
                return new AE_Var(varName);
            throw new AttributeSyntaxError("Expected RBRACE: "+t2);
        }
        throw new AttributeSyntaxError("Not recognized: "+tokenizer.peek());
    }

    private AttributeExpr readExprRel() {
        if ( tokenizer.eof() )
            throw new AttributeSyntaxError("END");
        AE_Attribute expr1 = readAttribute();
        // MaybeOneOf to follow.
        if ( tokenizer.eof() )
            return expr1;
        Token relToken = tokenizer.peek();

        // Is it a relationship token?
        switch ( relToken.getType() ) {
            // OK
            case LT, LE, GT, GE, EQ, NE:
                break;
            // Plain term variable
            default: return expr1;
        }

        /*Token relToken = */tokenizer.next();
        if ( tokenizer.eof() )
            throw new AttributeSyntaxError("Expected a relationship operator: '<', '=', '>', '<=', '>='");

        AE_AttrValue expr2 = readAttributeValue();

        Operator op = switch ( relToken.getType() ) {
            case EQ ->  Operator.EQ;
            //case EQUIVALENT ->  Operator.EQ;
            case LT -> Operator.LT;
            case LE ->  Operator.LE;
            case GT ->  Operator.GT;
            case GE -> Operator.GE;
            case NE -> Operator.NE;
            // Not OK.
            default -> throw new AttributeSyntaxError("Not a relationship operator: '"+relToken+"'");
        };
        return new AE_RelAny(relToken.getImage(), op, expr1, expr2);
    }

    /**
     * An attribute is a word starting with a letter, or a quoted string.
     */
    private AE_Attribute readAttribute() {
        Token peek = tokenizer.peek();
        TokenType peekType = peek.getType();

        if ( peek.isNumber() )
            throw new AttributeSyntaxError("Expected an attribute: Got a number: "+peek);

        if ( peek.isWord() || peek.isString() ) {
            Token t = tokenizer.next();
            String str = t.getImage();
            switch(str) {
                case kwTRUE, kwFALSE:
                    throw new AttributeSyntaxError("Found keyword '"+str+"', not an attribute");
            }
            return AE_Attribute.create(str);
        }
        throw new AttributeSyntaxError("Expected an attribute: Not recognized: "+peek);
    }

    /**
     * An attribute value is a word (starts with a letter) or string, same as an attribute,
     * or a number. It can be a keyword.
     */
    private AE_AttrValue readAttributeValue() {
        Token peek = tokenizer.peek();
        TokenType peekType = peek.getType();
        if ( peek.isWord() || peek.isString() || peek.isNumber() ) {
            Token t = tokenizer.next();
            String str = t.getImage();
            return AE_AttrValue.create(str);
        }
        throw new AttributeSyntaxError("Expected an attribute value: Not recognized: "+peek);
    }

    <X> List<X> parseList(Function<AttributeParserEngine, X> parseOneItem) {
        return parseList(parseOneItem, TokenType.COMMA);
    }

    <X> List<X> parseList(Function<AttributeParserEngine, X> parseOneItem, TokenType separator) {
        if ( endOfTokens() )
            // Empty.
            return List.of();

        List<X> acc = new ArrayList<>();
        for(;;) {
            X e = parseOneItem.apply(this);
            acc.add(e);
            // More?
            Token t2 = tokenizer.peek();
            if ( t2 == null )
                break;
            if ( t2.getType() != separator )
                break;
            // Read separator
            tokenizer.next();
            if ( ! tokenizer.hasNext() ) {
                // It was a trailing separator.
                throw new AttributeSyntaxError("Trailing comma");
            }
        }
        if ( ! tokenizer.eof() )
            throw new AttributeSyntaxError("Trailing content: "+tokenizer.peek()+" ...");
        return acc;
    }
}
