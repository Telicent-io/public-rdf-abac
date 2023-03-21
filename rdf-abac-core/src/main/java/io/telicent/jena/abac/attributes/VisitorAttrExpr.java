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

import io.telicent.jena.abac.attributes.syntax.*;
import io.telicent.jena.abac.attributes.syntax.AE_Attribute;

public interface VisitorAttrExpr {
//    public void visit(AE_Allow element);
//    public void visit(AE_Deny element);
//
//    public void visit(AE_Attribute element);
//    public void visit(AE_Var element);
//
//    //public void visit(AE1 element);
//
//    public void visit(AE_Bracketted element);
//
//    public void visit(AE_And element);
//    public void visit(AE_Or element);
//    public void visit(AE_RelAny element);

    public default void visit(AE_Allow element) {}
    public default void visit(AE_Deny element) {}

    public default void visit(AE_Attribute element) {}
    public default void visit(AE_Var element) {}

    //public void visit(AE1 element) {}

    public default void visit(AE_Bracketted element) {}

    public default void visit(AE_And element) {}
    public default void visit(AE_Or element) {}
    public default void visit(AE_RelAny element) {}

}
