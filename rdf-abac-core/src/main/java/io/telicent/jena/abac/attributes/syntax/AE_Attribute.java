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

package io.telicent.jena.abac.attributes.syntax;

import java.util.Collection;
import java.util.Objects;

import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.attributes.VisitorAttrExpr;
import io.telicent.jena.abac.attributes.syntax.tokens.Words;
import io.telicent.jena.abac.core.CxtABAC;
import org.apache.jena.atlas.io.IndentedWriter;

/**
 * A free-standing attribute, not part of an attribute expression.
 * <p>
 * This is looked up in the execution security context to evaluate.
 */

public class AE_Attribute implements AttributeExpr {
    private Attribute attribute;

    public static AE_Attribute create(String name) {
        return new AE_Attribute(name);
    }

    private AE_Attribute(String name) {
        this.attribute = new Attribute(name);
    }

    public Attribute attribute() { return attribute; }

    public String name() { return attribute.name(); }

    // Evaluate when on it's own, not in a attribute relationship expression.
    @Override
    public ValueTerm eval(CxtABAC cxt) {
        return eval(attribute, cxt);
    }

    static ValueTerm eval(Attribute attr, CxtABAC cxt) {
        Collection<ValueTerm> vt = cxt.getValue(attr);
        if ( vt == null )
            return ValueTerm.FALSE;
        return ValueTerm.value(vt.contains(ValueTerm.TRUE));

//        if ( vt == ValueTerm.TRUE )
//            return ValueTerm.TRUE;
//        // Has a non-boolean value in the context.
//        // Includes explicit.attribute-false
//        Log.info(AE_Attribute.class, "Looking up a plain attribute but the environment as value '+vt+'");
//        return vt;
    }

    @Override
    public void print(IndentedWriter w) {
        Words.print(w, attribute.name());
    }

    @Override
    public void visitor(VisitorAttrExpr visitor) {visitor.visit(this); }

    @Override
    public int hashCode() {
        return Objects.hash(attribute);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AE_Attribute other = (AE_Attribute)obj;
        return Objects.equals(attribute, other.attribute);
    }

    @Override
    public String toString() {
        return Words.wordStr(attribute.name());
    }
}
