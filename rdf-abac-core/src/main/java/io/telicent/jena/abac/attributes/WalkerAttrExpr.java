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

public class WalkerAttrExpr {

    public static void walk(AttributeExpr aExpr, VisitorAttrExpr visitor) {
        VisitorAttrExpr walker = new Walker(visitor);
        aExpr.visitor(walker);
    }

    static class Walker implements VisitorAttrExpr {

        private final VisitorAttrExpr visitor;

        Walker(VisitorAttrExpr visitor) {
            this.visitor = visitor;
        }

        @Override
        public void visit(AE_Allow element) { element.visitor(visitor); }

        @Override
        public void visit(AE_Deny element)  { element.visitor(visitor); }

        @Override
        public void visit(AE_Attribute element) { element.visitor(visitor); }

        @Override
        public void visit(AE_Var element)  { element.visitor(visitor); }

        @Override
        public void visit(AE_Bracketted element) { element.get().visitor(this); }

        @Override
        public void visit(AE_And element) {
            element.left().visitor(this);
            element.right().visitor(this);
            element.visitor(visitor);
        }

        @Override
        public void visit(AE_Or element) {
            element.left().visitor(this);
            element.right().visitor(this);
            element.visitor(visitor);
        }

        @Override
        public void visit(AE_RelAny element) { element.visitor(visitor); }
    }
}
