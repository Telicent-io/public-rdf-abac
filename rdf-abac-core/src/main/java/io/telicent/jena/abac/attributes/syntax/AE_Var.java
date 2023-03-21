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

import java.util.Objects;

import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.attributes.VisitorAttrExpr;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.core.CxtABAC;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.NotImplemented;

/**
 * Variable that substitutes a context sensitive value.
 * This is looked up in the execution security context to evaluate.
 */
public class AE_Var implements AttributeExpr {
    private String varName;

    public AE_Var(String varName) {
        this.varName = varName;
    }

    @Override
    public ValueTerm eval(CxtABAC cxt) {
        throw new NotImplemented();
//        Attribute attr = cxt.getVar(varName);
//        if ( attr == null )
//            return AttrExprValue.FALSE;
//        return AttrExprValue.create(attr.name());
    }

    @Override
    public String toString() {
        return "{"+varName+"}";
    }

    @Override
    public void print(IndentedWriter w) {
        w.write(toString());
    }

    @Override
    public void visitor(VisitorAttrExpr visitor) {visitor.visit(this); }

    @Override
    public int hashCode() {
        return Objects.hash(varName);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AE_Var other = (AE_Var)obj;
        return Objects.equals(varName, other.varName);
    }
}
