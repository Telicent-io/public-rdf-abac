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
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.core.CxtABAC;
import org.apache.jena.atlas.io.IndentedWriter;

public abstract class AE1 implements AttributeExpr {

    protected AttributeExpr attrExpr;

    protected AE1(AttributeExpr attrExpr) {
        this.attrExpr = attrExpr;
    }

    @Override
    public ValueTerm eval(CxtABAC cxt) {
        ValueTerm subValue = attrExpr.eval(cxt);
        ValueTerm value = eval(subValue, cxt) ;
        return value;
    }

    protected abstract ValueTerm eval(ValueTerm subValue, CxtABAC cxt);

    protected abstract String sym();

    public AttributeExpr get() { return attrExpr; }

    @Override
    public void print(IndentedWriter out) {
        out.write(sym());
        out.write(" ");
        attrExpr.print(out);
    }

    @Override
    public String toString() {
        return "("+sym()+" "+attrExpr+")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(attrExpr);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AE1 other = (AE1)obj;
        return Objects.equals(attrExpr, other.attrExpr);
    }
}
