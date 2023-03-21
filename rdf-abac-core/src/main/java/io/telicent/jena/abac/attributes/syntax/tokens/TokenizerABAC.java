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

import static org.apache.jena.atlas.lib.Chars.*;
import static org.apache.jena.riot.system.RiotChars.*;

import java.util.NoSuchElementException;
import java.util.Objects;

import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import org.apache.jena.atlas.AtlasException;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.io.PeekReader;
import org.apache.jena.riot.system.ErrorHandler;

/**
 * Tokenizer for the ABAC Attribute Labels.
 * Supports addition tokens.
 * Derived from TokenizerText in Jena.
 * Label tokenizing is a much simpler task.
 */
public final class TokenizerABAC implements Tokenizer
{
    // Drop through to final general symbol/keyword reader, including <=, !=
    // Care with <=

    private Token token = null;
    private final StringBuilder stringBuilder = new StringBuilder(200);
    private final PeekReader reader;
    // Whether whitespace between tokens includes newlines (in various forms).
    private final boolean lineMode;
    private boolean finished = false;

    // Not in jena 4.5.0, Replace at 4.6.0
    private static final char CH_EMARK        = '!' ;

    // The code assumes that errors throw exception and so stop parsing.
    private final ErrorHandler errorHandler;

    public static TokenizeLabelsBuilder create() { return new TokenizeLabelsBuilder() ; }

    public static Tokenizer fromString(String string) { return create().fromString(string).build(); }

    /*package*/ static TokenizerABAC internal(PeekReader reader, boolean lineMode, ErrorHandler errorHandler) {
        return new TokenizerABAC(reader, lineMode, errorHandler);
    }

    private TokenizerABAC(PeekReader reader, boolean lineMode, ErrorHandler errorHandler) {
        this.reader = Objects.requireNonNull(reader, "PeekReader");
        this.lineMode = lineMode;
        this.errorHandler = Objects.requireNonNull(errorHandler, "ErrorHandler");
    }

    @Override
    public final boolean hasNext() {
        if ( finished )
            return false;
        if ( token != null )
            return true;

        try {
            skip();
            if ( reader.eof() ) {
                // close();
                finished = true;
                return false;
            }
            token = parseToken();
            if ( token == null ) {
                // close();
                finished = true;
                return false;
            }
            return true;
        } catch (AtlasException ex) {
            if ( ex.getCause() != null ) {
                if ( ex.getCause().getClass() == java.nio.charset.MalformedInputException.class )
                    error("Bad character encoding", reader.getLineNum(), reader.getColNum());
                error("Bad input stream [" + ex.getCause() + "]", reader.getLineNum(), reader.getColNum());
            }
            error("Bad input stream", reader.getLineNum(), reader.getColNum());
            return false;
        }
    }

    @Override
    public final boolean eof() {
        return !hasNext();
    }

    @Override
    public final Token next() {
        if ( !hasNext() )
            throw new NoSuchElementException();
        Token t = token;
        token = null;
        return t;
    }

    @Override
    public final Token peek() {
        if ( !hasNext() )
            return null;
        return token;
    }

    @Override
    public void close() {
        IO.close(reader);
    }

    // ---- Machinery

    private void skip() {
        int ch = EOF;
        for (;;) {
            if ( reader.eof() )
                return;

            ch = reader.peekChar();
            if ( ch == CH_HASH ) {
                reader.readChar();
                // Comment. Skip to NL
                for (;;) {
                    ch = reader.peekChar();
                    if ( ch == EOF || isNewlineChar(ch) )
                        break;
                    reader.readChar();
                }
            }

            // Including excess newline chars from comment.
            if ( lineMode ) {
                if ( !isHorizontalWhitespace(ch) )
                    break;
            } else {
                if ( !isWhitespace(ch) )
                    break;
            }
            reader.readChar();
        }
    }

    private Token parseToken() {
        token = new Token(getLine(), getColumn());

        int ch = reader.peekChar();

//        // ---- IRI
//        // Maybe switch to "longest wins"
//        if ( ch == CH_LT ) {
//            // Look ahead on char
//            reader.readChar();
//            int chPeek = reader.peekChar();
//            // not '=', or space
//            if ( chPeek != ' ' && chPeek != '=' ) {
//                token.setImage(readIRI());
//                token.setType(TokenType.IRI);
//                return token;
//            }
//            // Dropthrough.
//            reader.pushbackChar(CH_LT);
//            //fatal("Internal error - parsed '%c' after '<'", chPeek);
//        }

        // ---- Literal
        if ( ch == CH_QUOTE1 || ch == CH_QUOTE2 ) {
            // The token type is STRING.
            // We incorporate this into a token for LITERAL_LANG or LITERAL_DT.
            token.setType(TokenType.STRING);

            reader.readChar();
            int ch2 = reader.peekChar();
            if ( ch2 == ch ) {
                reader.readChar(); // Read potential second quote.
                int ch3 = reader.peekChar();
                if ( ch3 == ch ) {
                    reader.readChar();     // Read potential third quote.
                    token.setImage(readLongString(ch, false));
                    StringType st = (ch == CH_QUOTE1) ? StringType.LONG_STRING1 : StringType.LONG_STRING2;
                    token.setStringType(st);
                } else {
                    // Two quotes then a non-quote.
                    // Must be '' or ""
                    // No need to pushback characters as we know the lexical
                    // form is the empty string.
                    // if ( ch2 != EOF ) reader.pushbackChar(ch2);
                    // if ( ch1 != EOF ) reader.pushbackChar(ch1); // Must be
                    // '' or ""
                    token.setImage("");
                    StringType st = (ch == CH_QUOTE1) ? StringType.STRING1 : StringType.STRING2;
                    token.setStringType(st);
                }
            } else {
                // One quote character.
                token.setImage(readString(ch, ch));
                // Record exactly what form of STRING was seen.
                StringType st = (ch == CH_QUOTE1) ? StringType.STRING1 : StringType.STRING2;
                token.setStringType(st);
            }
            return token;
        }

        // Other single and start chars.
        switch(ch)
        {
            case CH_SEMICOLON:  return oneChar(TokenType.SEMICOLON, CH_SEMICOLON);
            case CH_COMMA:      return oneChar(TokenType.COMMA, CH_COMMA);

            case CH_LBRACE:     return oneChar(TokenType.LBRACE, CH_LBRACE);
            case CH_RBRACE:     return oneChar(TokenType.RBRACE, CH_RBRACE);

            case CH_LPAREN:     return oneChar(TokenType.LPAREN, CH_LPAREN);
            case CH_RPAREN:     return oneChar(TokenType.RPAREN, CH_RPAREN);
            case CH_LBRACKET:   return oneChar(TokenType.LBRACKET, CH_LBRACKET);
            case CH_RBRACKET:   return oneChar(TokenType.RBRACKET, CH_RBRACKET);

            case CH_SLASH:      return oneChar(TokenType.SLASH, CH_SLASH);
            case CH_RSLASH:     return oneChar(TokenType.RSLASH, CH_RSLASH);

            case CH_COLON:      return oneChar(TokenType.COLON, CH_COLON);
            case CH_STAR:       return oneChar(TokenType.STAR, CH_STAR);
            case CH_QMARK:      return oneChar(TokenType.QMARK, CH_QMARK);

            // Multi-character symbols
            // Two character tokens : !=, GE >= , LE <=, &&, ||
            case CH_EQUALS:     return maybeTwoChar(CH_EQUALS, '=', TokenType.EQ, "=", TokenType.EQUIVALENT, "==");
            case CH_EMARK:      return maybeTwoChar(CH_EMARK, '=', TokenType.EMARK, "!", TokenType.NE, "!=");
            case CH_LT:         return maybeTwoChar(CH_LT, '=', TokenType.LT, "<", TokenType.LE, "<=");
            case CH_GT:         return maybeTwoChar(CH_GT, '=', TokenType.GT, ">", TokenType.GE, ">=");

            case CH_VBAR:       return maybeTwoChar(CH_VBAR, '|', TokenType.VBAR, "|", TokenType.LOGICAL_OR, "||");
            case CH_AMPHERSAND: return maybeTwoChar(CH_AMPHERSAND, '&', TokenType.AMPERSAND, "&", TokenType.LOGICAL_AND, "&&");
        }

        if ( isNewlineChar(ch) ) {
            do {
                int ch2 = reader.readChar();
                // insertCodepointDirect(stringBuilder,ch2);
            } while (isNewlineChar(reader.peekChar()));
            token.setType(TokenType.NL);
            //** token.setImage(stringBuilder.toString());
            return token;
        }

        // Numbers - maybe.
        /*
         * Turtle syntax rules.
            integer         ::=     ('-' | '+') ? [0-9]+
            double          ::=     ('-' | '+') ? ( [0-9]+ '.' [0-9]* exponent | '.' ([0-9])+ exponent | ([0-9])+ exponent )
            decimal         ::=     ('-' | '+')? ( [0-9]+ '.' [0-9]* | '.' ([0-9])+ | ([0-9])+ )
                                        0.0 .0 0.
        [19]    exponent        ::=     [eE] ('-' | '+')? [0-9]+
        []      hex             ::=     0x0123456789ABCDEFG
        */
        if ( ch == CH_PLUS || ch == CH_MINUS ) {
            reader.readChar();
            // Peek for base plus and minus.
            int ch2 = reader.peekChar();

            if ( !range(ch2, '0', '9') ) {
                // ch was end of symbol.
                // reader.readChar();
                if ( ch == CH_PLUS )
                    token.setType(TokenType.PLUS);
                else
                    token.setType(TokenType.MINUS);
                return token;
            }
            // Fall through
        }

        if ( ch == CH_PLUS || ch == CH_MINUS || range(ch, '0', '9') ) {
            // readNumberNoSign
            readNumber();
            return token;
        }

        // Plain words
        if ( Words.isWordStart(ch) ) {
            String str = readWord();
            token.setType(TokenType.WORD);
            token.setImage(str);
            return token;
        }
        fatal("Bad character: %c", (char)ch);
        return null;
    }

    private Token oneChar(TokenType tokenType, char character) {
        reader.readChar();
        token.setType(tokenType);
        token.setImage(character);
        return token;
    }

    private Token maybeTwoChar(char ch1, char secondChar, TokenType oneCharToken, String oneCharStr, TokenType twoCharToken, String twoCharStr) {
        reader.readChar();
        int ch2 = reader.peekChar();
        if ( ch2 == secondChar ) {
            reader.readChar();
            token.setType(twoCharToken);
            token.setImage(twoCharStr);
            return token;
        }
        token.setType(oneCharToken);
        token.setImage(oneCharStr);
        return token;
    }

    private static final boolean VeryVeryLaxIRI = false;
    // Spaces in IRI are illegal.
    private static final boolean AllowSpacesInIRI = false;

    // [8]  IRIREF  ::= '<' ([^#x00-#x20<>"{}|^`\] | UCHAR)* '>'
    private String readIRI() {
        stringBuilder.setLength(0);
        for (;;) {
            int ch = reader.readChar();
            switch(ch) {
                case EOF:
                    fatal("Broken IRI (End of file)"); return null;
                case NL:
                    fatal("Broken IRI (newline): %s", stringBuilder.toString()); return null;
                case CR:
                    fatal("Broken IRI (CR): %s", stringBuilder.toString()); return null;
                case CH_GT:
                    // Done!
                    return stringBuilder.toString();
                case CH_RSLASH:
                    if ( VeryVeryLaxIRI )
                        // Includes unicode escapes and also \n etc
                        ch = readLiteralEscape();
                    else
                        // NORMAL
                        ch = readUnicodeEscape();
                    // Don't check legality of ch (strict syntax at this point).
                    // That does not mean it is a good idea to bypass checking.
                    // Bad characters will lead to trouble elsewhere.
                    break;
                case CH_LT:
                    // Probably a corrupt file so treat as fatal.
                    fatal("Bad character in IRI (bad character: '<'): <%s[<]...>", stringBuilder.toString()); return null;
                case TAB:
                    error("Bad character in IRI (Tab character): <%s[tab]...>", stringBuilder.toString()); return null;
                case '{': case '}': case '"': case '|': case '^': case '`' :
                    if ( ! VeryVeryLaxIRI )
                        warning("Illegal character in IRI (codepoint 0x%02X, '%c'): <%s[%c]...>", ch, (char)ch, stringBuilder.toString(), (char)ch);
                    break;
                case SPC:
                    if ( ! AllowSpacesInIRI )
                        error("Bad character in IRI (space): <%s[space]...>", stringBuilder.toString());
                    else
                        warning("Bad character in IRI (space): <%s[space]...>", stringBuilder.toString());
                    break;
                default:
                    if ( ch <= 0x19 )
                        warning("Illegal character in IRI (control char 0x%02X): <%s[0x%02X]...>", ch, stringBuilder.toString(), ch);

            }
            if ( ! VeryVeryLaxIRI && ch >= 0xA0 && ! isUcsChar(ch) )
                warning("Illegal character in IRI (Not a ucschar: 0x%04X): <%s[U+%04X]...>", ch, stringBuilder.toString(), ch);
            insertCodepoint(stringBuilder, ch);
        }
    }

    private static boolean isUcsChar(int ch) {
        // RFC 3987
        // ucschar    = %xA0-D7FF / %xF900-FDCF / %xFDF0-FFEF
        //            / %x10000-1FFFD / %x20000-2FFFD / %x30000-3FFFD
        //            / %x40000-4FFFD / %x50000-5FFFD / %x60000-6FFFD
        //            / %x70000-7FFFD / %x80000-8FFFD / %x90000-9FFFD
        //            / %xA0000-AFFFD / %xB0000-BFFFD / %xC0000-CFFFD
        //            / %xD0000-DFFFD / %xE1000-EFFFD
        boolean b = range(ch, 0xA0, 0xD7FF)  || range(ch, 0xF900, 0xFDCF)  || range(ch, 0xFDF0, 0xFFEF);
        if ( b )
            return true;
        if ( ch < 0x1000 )
            return false;
        // 32 bit checks.
        return
            range(ch, 0x10000, 0x1FFFD) || range(ch, 0x20000, 0x2FFFD) || range(ch, 0x30000, 0x3FFFD) ||
            range(ch, 0x40000, 0x4FFFD) || range(ch, 0x50000, 0x5FFFD) || range(ch, 0x60000, 0x6FFFD) ||
            range(ch, 0x70000, 0x7FFFD) || range(ch, 0x80000, 0x8FFFD) || range(ch, 0x90000, 0x9FFFD) ||
            range(ch, 0xA0000, 0xAFFFD) || range(ch, 0xB0000, 0xBFFFD) || range(ch, 0xC0000, 0xCFFFD) ||
            range(ch, 0xD0000, 0xDFFFD) || range(ch, 0xE1000, 0xEFFFD);
    }

    // Read a unicode escape : does not allow \\ bypass
    private final int readUnicodeEscape() {
        int ch = reader.readChar();
        if ( ch == EOF )
            fatal("Broken escape sequence");

        switch (ch) {
            case 'u': return readUnicode4Escape();
            case 'U': return readUnicode8Escape();
            default:
                fatal("Illegal unicode escape sequence value: \\%c (0x%02X)", ch, ch);
        }
        return 0;
    }

    // Get characters between two markers.
    // strEscapes may be processed
    private String readString(int startCh, int endCh) {
        // Position at start of string.
        stringBuilder.setLength(0);
        // Assumes first delimiter char read already.
        // Reads terminating delimiter

        for (;;) {
            int ch = reader.readChar();

            // Raw replacement char in a string.
            if ( ch == REPLACEMENT )
                warning("Unicode replacement character U+FFFD in string");
            else if ( ch == EOF ) {
                // if ( endNL ) return stringBuilder.toString();
                fatal("Broken token: %s", stringBuilder.toString());
            }
            else if ( ch == NL )
                fatal("Broken token (newline): %s", stringBuilder.toString());
            else if ( ch == endCh )
                return stringBuilder.toString();
            else if ( ch == CH_RSLASH )
                // Allow escaped replacement character.
                ch = readLiteralEscape();

            insertCodepoint(stringBuilder, ch);
        }
    }

    private String readLongString(int quoteChar, boolean endNL) {
        stringBuilder.setLength(0);
        for (;;) {
            int ch = reader.readChar();
            if ( ch == REPLACEMENT )
                warning("Input has Unicode replacement character U+FFFD in string");
            else if ( ch == EOF ) {
                if ( endNL )
                    return stringBuilder.toString();
                fatal("Broken long string");
            }
            else if ( ch == quoteChar ) {
                if ( threeQuotes(quoteChar) )
                    return stringBuilder.toString();
            } else if ( ch == CH_RSLASH )
                ch = readLiteralEscape();
            insertCodepoint(stringBuilder, ch);
        }
    }

    // Assume we have read the first quote char.
    // On return:
    //   If false, have moved over no more characters (due to pushbacks)
    //   If true, at end of 3 quotes
    private boolean threeQuotes(int ch) {
        // reader.readChar(); // Read first quote.
        int ch2 = reader.peekChar();
        if ( ch2 != ch ) {
            // reader.pushbackChar(ch2);
            return false;
        }

        reader.readChar(); // Read second quote.
        int ch3 = reader.peekChar();
        if ( ch3 != ch ) {
            // reader.pushbackChar(ch3);
            reader.pushbackChar(ch2);
            return false;
        }

        // Three quotes.
        reader.readChar(); // Read third quote.
        return true;
    }

    /*
     * [146]  INTEGER  ::=  [0-9]+
     * [147]  DECIMAL  ::=  [0-9]* '.' [0-9]+
     * [148]  DOUBLE  ::=  [0-9]+ '.' [0-9]* EXPONENT | '.' ([0-9])+ EXPONENT | ([0-9])+ EXPONENT
     * []     hex             ::=     0x0123456789ABCDEFG
     */
    private void readNumber() {
        // One entry, definitely a number.
        // Already dealt with '.' as a (non) decimal.
        boolean isDouble = false;
        boolean isDecimal = false;
        stringBuilder.setLength(0);

        int x = 0; // Digits before a dot.
        int ch = reader.peekChar();
        if ( ch == '0' ) {
            x++;
            reader.readChar();
            insertCodepointDirect(stringBuilder, ch);
            ch = reader.peekChar();
            if ( ch == 'x' || ch == 'X' ) {
                reader.readChar();
                insertCodepointDirect(stringBuilder, ch);
                readHex(reader, stringBuilder);
                token.setImage(stringBuilder.toString());
                token.setType(TokenType.HEX);
                return;
            }
        } else if ( ch == '-' || ch == '+' ) {
            readPossibleSign(stringBuilder);
        }

        x += readDigits(stringBuilder);
        ch = reader.peekChar();
        if ( ch == CH_DOT ) {
            reader.readChar();
            stringBuilder.append(CH_DOT);
            isDecimal = true; // Includes things that will be doubles.
            readDigits(stringBuilder);
        }

        if ( x == 0 && !isDecimal )
            // Possible a tokenizer error - should not have entered readNumber
            // in the first place.
            fatal("Unrecognized as number");

        if ( exponent(stringBuilder) ) {
            isDouble = true;
            isDecimal = false;
        }

        // Final part - "decimal" 123. is an integer 123 and a DOT.
        if ( isDecimal ) {
            int len = stringBuilder.length();
            if ( stringBuilder.charAt(len - 1) == CH_DOT ) {
                stringBuilder.setLength(len - 1);
                reader.pushbackChar(CH_DOT);
                isDecimal = false;
            }
        }

        token.setImage(stringBuilder.toString());
        if ( isDouble )
            token.setType(TokenType.DOUBLE);
        else if ( isDecimal )
            token.setType(TokenType.DECIMAL);
        else
            token.setType(TokenType.INTEGER);
    }

    private void readHex(PeekReader reader, StringBuilder sb) {
        // Just after the 0x, which are in sb
        int x = 0;
        for (;;) {
            int ch = reader.peekChar();
            if ( !isHexChar(ch) )
                break;
            reader.readChar();
            insertCodepointDirect(sb, ch);
            x++;
        }
        if ( x == 0 )
            fatal("No hex characters after %s", sb.toString());
    }

    private int readDigits(StringBuilder buffer) {
        int count = 0;
        for (;;) {
            int ch = reader.peekChar();
            if ( !range(ch, '0', '9') )
                break;
            reader.readChar();
            insertCodepointDirect(buffer, ch);
            count++;
        }
        return count;
    }

    private void readPossibleSign(StringBuilder sb) {
        int ch = reader.peekChar();
        if ( ch == '-' || ch == '+' ) {
            reader.readChar();
            insertCodepointDirect(sb, ch);
        }
    }

    private boolean exponent(StringBuilder sb) {
        int ch = reader.peekChar();
        if ( ch != 'e' && ch != 'E' )
            return false;
        reader.readChar();
        insertCodepointDirect(sb, ch);
        readPossibleSign(sb);
        int x = readDigits(sb);
        if ( x == 0 )
            fatal("Malformed double: %s", sb);
        return true;
    }

    // -- Words, processed as a string of characters.
    // First character has been verified but not read yet.
    private String readWord() {
        // First char
        int firstCh = reader.readChar();
        //if ( ! Words.isWordStart(firstCh) ) {}

        stringBuilder.setLength(0);
        insertCodepoint(stringBuilder, firstCh);
        // First char is valid last char.
        int idx = 0;
        // Remember the index of the last seen character that is legal as a last character.
        int lastValidLastChar = 0;
        // Loop:middle and last character.
        int chRead = EOF;
        for (;;) {
            int ch = reader.peekChar();
            if ( ch == EOF )
                break;
            if ( ! Words.isWordMiddle(ch) )
                break;

            chRead = reader.readChar();
            idx++;
            insertCodepointDirect(stringBuilder, chRead);
            if ( Words.isWordEnd(ch) )
                lastValidLastChar = idx;
            // Loop
        }

        // Delete: note first character was valid
        if ( chRead != EOF && lastValidLastChar != idx ) {
            // Move backwards.
            for ( int i = idx ; i > lastValidLastChar ; i-- ) {
                int ch = stringBuilder.charAt(i);
                stringBuilder.deleteCharAt(i);
                reader.pushbackChar(chRead);
            }
        }
        return stringBuilder.toString();
    }

    // --

    private void insertCodepoint(StringBuilder buffer, int ch) {
        if ( Character.charCount(ch) == 1 )
            insertCodepointDirect(buffer, ch);
        else {
            // Convert to UTF-16. Note that the rest of any system this is used
            // in must also respect codepoints and surrogate pairs.
            if ( !Character.isDefined(ch) && !Character.isSupplementaryCodePoint(ch) )
                fatal("Illegal codepoint: 0x%04X", ch);
            char[] chars = Character.toChars(ch);
            buffer.append(chars);
        }
    }

    // Insert code point, knowing that 'ch' is 16 bit (basic plane)
    private static void insertCodepointDirect(StringBuilder buffer, int ch) {
        buffer.append((char)ch);
    }

    @Override
    public long getColumn() {
        return reader.getColNum();
    }

    @Override
    public long getLine() {
        return reader.getLineNum();
    }

    // ---- Escape sequences

    private final int readLiteralEscape() {
        int c = reader.readChar();
        if ( c == EOF )
            fatal("Escape sequence not completed");

        switch (c) {
            case 'n':   return NL;
            case 'r':   return CR;
            case 't':   return TAB;
            case 'f':   return '\f';
            case 'b':   return BSPACE;
            case '"':   return '"';
            case '\'':  return '\'';
            case '\\':  return '\\';
            case 'u':   return readUnicode4Escape();
            case 'U':   return readUnicode8Escape();
            default:
                fatal("Illegal escape sequence value: %c (0x%02X)", c, c);
                return 0;
        }
    }

    private final int readCharEscape() {
        // PN_LOCAL_ESC ::= '\' ( '_' | '~' | '.' | '-' | '!' | '$' | '&' | "'"
        //                | '(' | ')' | '*' | '+' | ',' | ';' | '=' | '/' | '?' | '#' | '@' | '%' )

        int c = reader.readChar();
        if ( c == EOF )
            fatal("Escape sequence not completed");

        switch (c) {
            case '_': case '~': case '.':  case '-':  case '!':  case '$':  case '&':
            case '\'':
            case '(':  case ')':  case '*':  case '+':  case ',':  case ';':
            case '=':  case '/':  case '?':  case '#':  case '@':  case '%':
                return c;
            default:
                fatal("illegal character escape value: \\%c", c);
                return 0;
        }
    }

    private final
    int readUnicode4Escape() { return readHexSequence(4); }

    private final int readUnicode8Escape() {
        int ch8 = readHexSequence(8);
        if ( ch8 > Character.MAX_CODE_POINT )
            fatal("Illegal code point in \\U sequence value: 0x%08X", ch8);
        return ch8;
    }

    private final int readHexSequence(int N) {
        int x = 0;
        for (int i = 0; i < N; i++) {
            int d = readHexChar();
            if ( d < 0 )
                return -1;
            x = (x << 4) + d;
        }
        return x;
    }

    private final int readHexChar() {
        int ch = reader.readChar();
        if ( ch == EOF )
            fatal("Not a hexadecimal character (end of file)");

        int x = valHexChar(ch);
        if ( x != -1 )
            return x;
        fatal("Not a hexadecimal character: '%c'", (char)ch);
        return -1;
    }

    private boolean expect(String str) {
        for (int i = 0; i < str.length(); i++) {
            char want = str.charAt(i);
            if ( reader.eof() ) {
                fatal("End of input during expected string: %s", str);
                return false;
            }
            int inChar = reader.peekChar();
            if ( inChar != want ) {
                fatal("expected \"%s\"", str);
                return false;
            }
            reader.readChar();
        }
        return true;
    }

    /** Warning - can continue. */
    private void warning(String message, Object... args) {
        String msg = String.format(message, args);
        errorHandler.warning(msg, reader.getLineNum(), reader.getColNum());
    }

    /**
     * Error - at the tokenizer level, it can continue (with some junk) but it is a serious error and the
     * caller probably should treat as an error and stop.
     * @param message
     * @param args
     */
    private void error(String message, Object... args) {
        String msg = String.format(message, args);
        errorHandler.error(msg, reader.getLineNum(), reader.getColNum());
        throw new AttributeSyntaxError(msg);
    }

    /** Structural error - unrecoverable - but reported as ERROR (FATAL can imply system fault) */
    private void fatal(String message, Object... args) {
        String msg = String.format(message, args);
        long line = reader.getLineNum();
        if ( line == 1 )
            // Lien is normally 1: pon't print.
            line = -1;
        errorHandler.fatal(msg, line, reader.getColNum());
        // We require that errors to cause the tokenizer to stop so in case the
        // provided error handler does not, we throw an exception.
        throw new AttributeSyntaxError(message);
    }
}
