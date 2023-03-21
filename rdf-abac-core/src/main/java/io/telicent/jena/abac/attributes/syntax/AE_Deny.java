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
import org.apache.jena.atlas.io.IndentedWriter;

public class AE_Deny implements AttributeExpr {

    private static AttributeExpr singelton = new AE_Deny();
    public static AttributeExpr value() {
        return singelton;
    }

    private AE_Deny() {}

    @Override
    public ValueTerm eval(CxtABAC cxt) {
        return ValueTerm.FALSE;
    }

    @Override
    public void print(IndentedWriter w) {
        w.print(toString());
    }

    @Override
    public void visitor(VisitorAttrExpr visitor) {visitor.visit(this); }

    @Override
    public String toString() {
        return AEX.strDENY;
    }
}
