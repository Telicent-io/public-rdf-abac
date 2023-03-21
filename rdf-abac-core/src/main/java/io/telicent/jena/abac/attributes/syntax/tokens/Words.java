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

import static org.apache.jena.atlas.lib.Chars.charInArray;
import static org.apache.jena.riot.system.RiotChars.isAlphaNumeric;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.Chars;
import org.apache.jena.atlas.lib.EscapeStr;

public class Words {
    //public static isWord

    // See also TokenizerABAC.readWord
    // Thgis is done with isWord*
    // extraFirstCharsWord must include extractLastCharsWord
    // extractLastCharsWord can not have chars not in extraRestCharsWord

    static /*package*/ char[] extraFirstCharsWord   = new char[] {'_'};
    static /*package*/ char[] extraMiddleCharsWord  = new char[] {'_', '.' ,'-', '+', ':'};
    static /*package*/ char[] extractLastCharsWord  = new char[] {'_'};

    public static boolean isWord(String string) {
        int N = string.length();
        if ( N == 0 )
            return false;
        for ( int i = 0 ; i < string.length()-1 ; i++ ) {
            char ch  = string.charAt(i);
            if ( i == 0 ) {
                if ( ! isWordStart(ch) )
                    return false;
                continue;
            }
            if ( ! isWordMiddle(ch) ) {
                return false;
            }
        }
        char ch  = string.charAt(N-1);
        if ( ! isWordEnd(ch) )
            return false;
        return true;
    }

    public static boolean isWordStart(int ch) {
        return isWordChar(ch, extraFirstCharsWord);
    }

    public static boolean isWordMiddle(int ch) {
        return isWordChar(ch, extraMiddleCharsWord);
    }

    public static boolean isWordEnd(int ch) {
        return isWordChar(ch, extractLastCharsWord);
    }

    // Test for A2N and extra characters.
    /*package*/ static boolean isWordChar(int ch, char[] extraChars) {
        return isAlphaNumeric(ch) || charInArray(ch, extraChars);
    }

    /** Return a syntax-valid string for the {@code str} value */
    public static String wordStr(String str) {
        if ( Words.isWord(str) )
            return str;
        return quotedStr(str);
    }

    /** Return a string for the {@code str} value: always quoted. */
    public static String quotedStr(String str) {
        String s = EscapeStr.stringEsc(str);
        return Chars.CH_QUOTE2+s+Chars.CH_QUOTE2;
    }

    public static void print(IndentedWriter w, String str) {
        if ( Words.isWord(str) ) {
            w.write(str);
            return;
        }
        String s = EscapeStr.stringEsc(str);
        w.write(Chars.CH_QUOTE2);
        w.write(s);
        w.write(Chars.CH_QUOTE2);
    }
}
