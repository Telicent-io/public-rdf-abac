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

package io.telicent.jena.abac.attributes.syntax.tokens;

import static io.telicent.jena.abac.attributes.syntax.tokens.TokenType.STRING;
import static org.apache.jena.atlas.lib.Chars.* ;

import java.util.Objects ;

import org.apache.jena.riot.RiotException ;

public final class Token
{
    private TokenType tokenType = null ;

    private String tokenImage = null ;
    private StringType stringType = null ;

    private long column ;
    private long line ;

    public final TokenType getType()        { return tokenType ; }
    public final String getImage()          { return tokenImage ; }
    //public final String getImage1()  { return tokenImage1 ; }

    public final StringType getStringType() { return stringType ; }
    public final boolean hasStringType(StringType st)   { return this.stringType == st ;}
    public final boolean isLongString() {
        switch(stringType) {
            case LONG_STRING1:
            case LONG_STRING2:
                return true;
                default:
                    return false;
        }
    }

    public final Token setType(TokenType tokenType)     { this.tokenType = tokenType ; return this ; }
    public final Token setImage(String tokenImage)      { this.tokenImage = tokenImage ; return this ; }
    public final Token setImage(char tokenImage)        { this.tokenImage = String.valueOf(tokenImage) ; return this ; }
    public final Token setStringType(StringType st)     { this.stringType = st ; return this ; }

    static Token create(String s) {
        Tokenizer tt = TokenizerABAC.create().fromString(s).build();
        if ( ! tt.hasNext() )
            throw new RiotException("No token") ;
        Token t = tt.next() ;
        if ( tt.hasNext() )
            throw new RiotException("Extraneous characters") ;
        return t ;
    }

    public long getColumn() {
        return column;
    }

    public long getLine() {
        return line;
    }

    public Token(String string) { this(STRING, string) ; }

    public Token(TokenType type) { this(type, null) ; }

    public Token(TokenType type, String image1) {
        this() ;
        setType(type) ;
        setImage(image1) ;
    }

    private Token() { this(-1, -1) ; }

    public Token(long line, long column) { this.line = line ; this.column = column ; }

    public Token(Token token) {
        this(token.tokenType, token.tokenImage) ;
        this.line           = token.line ;
        this.column         = token.column ;
    }

    // Convenience operations for accessing tokens.
    public String asString() {
        switch (tokenType)
        {
            case STRING:
                return getImage() ;
            default:
                return null ;
        }
    }

    public String asWord() {
        if ( !hasType(TokenType.WORD) && !hasType(TokenType.STRING) )
            return null;
        return tokenImage;
    }

    public String text() {
        return toString(false);
    }

    @Override
    public String toString() {
        return toString(false);
    }

    static final String delim1 = "" ;
    static final String delim2 = "" ;
    public String toString(boolean addLocation) {
        StringBuilder sb = new StringBuilder() ;
        if ( addLocation && getLine() >= 0 && getColumn() >= 0 )
            sb.append(String.format("[%d,%d]", getLine(), getColumn())) ;
        sb.append("[") ;
        if ( getType() == null )
            sb.append("null") ;
        else
            sb.append(getType().toString()) ;

        if ( getImage() != null )
        {
            sb.append(":") ;
            sb.append(delim1) ;
            sb.append(getImage()) ;
            sb.append(delim1) ;
        }
        sb.append("]") ;
        return sb.toString() ;
    }

    public boolean isEOF()      { return tokenType == TokenType.EOF ; }

    public boolean isWord()     { return tokenType == TokenType.WORD ; }

    public boolean isString()   { return tokenType == TokenType.STRING ; }

    public boolean isNumber() {
        switch (tokenType) {
            case DECIMAL :
            case DOUBLE :
            case INTEGER :
            case HEX :
                return true;
            default :
                return false;
        }
    }

    public boolean isIRI() {
        //return tokenType.equals(TokenType.IRI) || tokenType.equals(TokenType.PREFIXED_NAME);
        return tokenType.equals(TokenType.IRI); // || tokenType.equals(TokenType.PREFIXED_NAME);
    }

    public boolean hasType(TokenType tokenType) {
        return getType() == tokenType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringType, tokenImage, tokenType);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        Token other = (Token)obj;
        return stringType == other.stringType && Objects.equals(tokenImage, other.tokenImage) && tokenType == other.tokenType;
    }

    public static Token tokenForChar(char character) {
        switch (character) {
            case CH_DOT :
                return new Token(TokenType.DOT);
            case CH_SEMICOLON :
                return new Token(TokenType.SEMICOLON);
            case CH_COMMA :
                return new Token(TokenType.COMMA);
            case CH_LBRACE :
                return new Token(TokenType.LBRACE);
            case CH_RBRACE :
                return new Token(TokenType.RBRACE);
            case CH_LPAREN :
                return new Token(TokenType.LPAREN);
            case CH_RPAREN :
                return new Token(TokenType.RPAREN);
            case CH_LBRACKET :
                return new Token(TokenType.LBRACKET);
            case CH_RBRACKET :
                return new Token(TokenType.RBRACKET);
            default :
                throw new RuntimeException("Token error: unrecognized character: " + character);
        }
    }

    public static Token tokenForWord(String word) {
        return new Token(TokenType.WORD, word);
    }
}
