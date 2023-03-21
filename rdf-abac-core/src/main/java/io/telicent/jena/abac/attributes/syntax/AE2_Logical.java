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

/**
 * Two argument logical (boolean valued arguments) expression.
 * <p>
 * "&amp;" and "|".
 */
public abstract class AE2_Logical implements AttributeExpr {

    protected final AttributeExpr left;
    protected final AttributeExpr right;

    protected AE2_Logical(AttributeExpr left, AttributeExpr right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public ValueTerm eval(CxtABAC cxt) {
        return AttrExprEvaluator.evalAnd(left, right, cxt);
    }

    protected abstract String sym();

    public AttributeExpr left() { return left; }

    public AttributeExpr right() { return right; }

    @Override
    public void print(IndentedWriter out) {
        left.print(out);
        out.write(" ");
        out.write(sym());
        out.write(" ");
        right.print(out);
    }

    @Override
    public String toString() {
        return "("+sym()+" "+left+" "+right+")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AE2_Logical other = (AE2_Logical)obj;
        return Objects.equals(left, other.left) && Objects.equals(right, other.right);
    }
}
