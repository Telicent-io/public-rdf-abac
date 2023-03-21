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

import io.telicent.jena.abac.core.CxtABAC;
import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.io.IndentedWriter;

public interface AttributeExpr {

    public ValueTerm eval(CxtABAC context);

    public void print(IndentedWriter w);

    public default void print() {
        print(IndentedWriter.stdout);
        IndentedWriter.stdout.flush();
    }

    public default String str() {
        IndentedLineBuffer out = new IndentedLineBuffer();
        print(out);
        return out.asString();
    }

    public void visitor(VisitorAttrExpr visitor);

    @Override
    public int hashCode();

    @Override
    public boolean equals(Object other);
}
