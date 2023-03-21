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

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.attributes.syntax.tokens.Words;
import org.apache.jena.ext.com.google.common.collect.MultimapBuilder;
import org.apache.jena.ext.com.google.common.collect.SetMultimap;

/**
 * {@code AttributeValueSet} is request-side capabilities that are matched against attribute expressions (the data labelling).
 */
public class AttributeValueSet {

    public static AttributeValueSet EMPTY = new AttributeValueSet(List.of());

    // Attribute to a set of values.
    private final SetMultimap<Attribute, ValueTerm> attributes = MultimapBuilder.hashKeys().hashSetValues().build();

    private AttributeValueSet(Collection<AttributeValue>aValues) {
        aValues.forEach(av->this.attributes.put(av.attribute(), av.value()));
    }

    public static AttributeValueSet of(AttributeValue attrValue) {
        return new AttributeValueSet(List.of(attrValue));
    }

    public static AttributeValueSet of(Collection<AttributeValue> attrValues) {
        return new AttributeValueSet(attrValues);
    }

    // For test convenience
    public static AttributeValueSet of(String ... attributes) {
        List<AttributeValue> attrValues = new ArrayList<>(attributes.length);
        for ( String a : attributes ) {
            attrValues.add(AttributeValue.of(new Attribute(a), ValueTerm.TRUE));
        }
        return new AttributeValueSet(attrValues);
    }

    public Collection<Attribute> attributes() { return Collections.unmodifiableCollection(attributes.keySet()); }

    public void attributeValuesPairs(BiConsumer<Attribute, ValueTerm> action) {
        attributes.forEach(action);
    }

    public void attributeValues(Consumer<AttributeValue> action) {
        attributes.forEach((attr,value)->action.accept(AttributeValue.of(attr, value)));
    }


    public boolean hasAttribute(Attribute attr) {
        return attributes.containsKey(attr);
    }

    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    public Collection<ValueTerm> get(Attribute attribute) {
        return attributes.get(attribute);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        attributes.forEach((a,v)->sj.add(a.name()+"="+v.asString()));
        return "AttributeValueSet["+sj.toString()+"]";
    }

    public String asString() {
        StringJoiner sj = new StringJoiner(", ");
        attributes.forEach((a,v)->sj.add(Words.wordStr(a.name())+"="+v.asString()));
        return sj.toString();
    }


    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AttributeValueSet other = (AttributeValueSet)obj;
        return Objects.equals(attributes, other.attributes);
    }
}
