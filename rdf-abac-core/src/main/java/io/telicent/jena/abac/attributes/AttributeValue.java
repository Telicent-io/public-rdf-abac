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

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.attributes.syntax.tokens.Words;

// Not being a record means we can hide the constructor and do some testing.
public class AttributeValue {

    public static final ValueTerm dftTrue = ValueTerm.TRUE;

    private final Attribute attribute;
    private final ValueTerm value;

    public static AttributeValue of(String attribute, ValueTerm value) {
        return new AttributeValue(new Attribute(attribute), value);
    }

    public static AttributeValue of(Attribute attribute, ValueTerm value) {
        return new AttributeValue(attribute, value);
    }

    private AttributeValue(Attribute attribute, ValueTerm value) {
        this.attribute = Objects.requireNonNull(attribute, "Attribute");
        this.value = Objects.requireNonNull(value, "Value");
    }

    public Attribute attribute() {
        return attribute;
    }

    public ValueTerm value() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, value);
    }

    public String asString() {
        if ( ABAC.LEGACY ) {
            if ( dftTrue.equals(value) )
                // Write as legacy: no "="
                // It must stil be a valid RDF ABAC word.
                return Words.wordStr(attribute.name());
        }
        return Words.wordStr(attribute.name()) + "=" + value.asString();
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AttributeValue other = (AttributeValue)obj;
        return Objects.equals(attribute, other.attribute) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        return Words.wordStr(attribute.name()) + "=" + value;
    }
}
