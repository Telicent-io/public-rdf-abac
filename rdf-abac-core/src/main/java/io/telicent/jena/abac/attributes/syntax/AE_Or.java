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

import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.attributes.VisitorAttrExpr;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.core.CxtABAC;

public final class AE_Or extends AE2_Logical {
    public AE_Or(AttributeExpr left, AttributeExpr right) {
        super(left, right);
    }

    @Override
    public ValueTerm eval(CxtABAC cxt) {
        return AttrExprEvaluator.evalOr(left, right, cxt);
    }

    @Override
    protected String sym() {
        return "||";
    }

    @Override
    public void visitor(VisitorAttrExpr visitor) {visitor.visit(this); }
}