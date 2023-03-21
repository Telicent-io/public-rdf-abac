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

import java.util.Objects;

import io.telicent.jena.abac.attributes.syntax.tokens.Words;
import org.apache.jena.atlas.io.IndentedWriter;

public class ValueTerm {
    public static ValueTerm TRUE = new ValueTerm(true);
    public static ValueTerm FALSE = new ValueTerm(false);

    private final boolean booleanValue;
    private final String string;

    public static ValueTerm value(boolean b) {
        return b ? TRUE : FALSE ;
    }

    public static ValueTerm value(String str) {
        return new ValueTerm(str);
    }

    private ValueTerm(boolean val) {
        this.booleanValue = val;
        this.string = null;
    }

    private ValueTerm(String str) {
        this.booleanValue = false;
        this.string = str;
    }

    public boolean isString() { return string != null; }
    public boolean isBoolean() { return string == null; }

    public String getString() {
        if ( ! isString() )
            throw new AttributeException("Not a string value");
        return string;
    }

    public boolean getBoolean() {
        if ( ! isBoolean() )
            throw new AttributeException("Not a boolean value");
        return booleanValue;
    }

    /** As a string - legal syntax */
    public String asString() {
        if ( isBoolean() )
            return booleanValue ? "true" : "false";
      return Words.wordStr(string);
    }

    //@Override
    public void print(IndentedWriter w) {
        if ( isBoolean() )
            w.print(booleanValue ? "true" : "false");
        else
            Words.print(w, string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(booleanValue, string);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        ValueTerm other = (ValueTerm)obj;
        return booleanValue == other.booleanValue && Objects.equals(string, other.string);
    }

    @Override
    public String toString() {
        return asString();
    }
}
