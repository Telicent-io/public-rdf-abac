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

import static io.telicent.jena.abac.Hierarchy.Comparison.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.telicent.jena.abac.Hierarchy.Comparison;
import io.telicent.jena.abac.attributes.AttributeException;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.junit.jupiter.api.Test;

public class TestHierarchy {

    @Test public void hierarchy_00() {
        Hierarchy h0 = Hierarchy.create("HIER");
    }

    @Test public void hierarchy_01() {
        assertThrows(IllegalArgumentException.class, () -> Hierarchy.create("HIER:"));
    }

    @Test public void hierarchy_02() {
        Hierarchy h = Hierarchy.create("HIER", "A");
        checkHierarchy(h);
    }

    @Test public void hierarchy_03() {
        Hierarchy h = Hierarchy.create("HIER", "A", "B", "C");
        checkHierarchy(h);
    }

    @Test public void hierarchy_parse_00() {
        assertThrows(AttributeException.class, () -> Hierarchy.fromString("HIER") ) ;
    }

    @Test public void hierarchy_parse_01() {
        Hierarchy h = Hierarchy.fromString("HIER:");
    }

    @Test public void hierarchy_parse_02() {
        Hierarchy h = Hierarchy.fromString("HIER: A");
        checkHierarchy(h);
    }

    @Test public void hierarchy_parse_03() {
        Hierarchy h = Hierarchy.fromString("HIER: A, \"With space\", \"With,comma\", C");
        checkHierarchy(h);
    }

    private void checkHierarchy(Hierarchy hierarchy) {
        String x = hierarchy.asString();
        Hierarchy hierarchy2 = Hierarchy.fromString(x);
        assertEquals(hierarchy, hierarchy2);
    }

    ValueTerm av0 = ValueTerm.value("A");
    ValueTerm av1 = ValueTerm.value("B");
    ValueTerm av2 = ValueTerm.value("C");
    Hierarchy h = Hierarchy.fromString("Test: A, B, C");

    @Test public void  hierarchy_compare_01() {
        compare(EQ, h, av0, av0);
    }

    @Test public void  hierarchy_compare_02() {
        compare(EQ, h, av1, av1);
    }

    @Test public void  hierarchy_compare_03() {
        compare(EQ, h, av2, av2);
    }

    @Test public void  hierarchy_compare_04() {
        compare(LT, h, av0, av1);
        compare(GT, h, av1, av0);
    }

    @Test public void  hierarchy_compare_05() {
        compare(LT, h, av0, av2);
        compare(GT, h, av2, av0);
    }

    @Test public void  hierarchy_compare_06() {
        compare(LT, h, av1, av2);
        compare(GT, h, av2, av1);
    }

    @Test public void  hierarchy_compare_10() {
        ValueTerm avx = ValueTerm.value("X");
        compare(NONE, h, avx, av2);
        compare(NONE, h, av2, avx);
    }

    @Test public void  hierarchy_compare_11() {
        ValueTerm avx = ValueTerm.value("X");
        ValueTerm avy = ValueTerm.value("Y");
        compare(NONE, h, avx, avy);
    }

    private static void compare(Comparison expected, Hierarchy h, ValueTerm av1, ValueTerm av2) {
        Comparison actual = h.compareTo(av1, av2);
        assertEquals(expected, actual);
        //System.out.printf("%s :: compare(%s, %s) -> %s\n", h, av1, av2, actual);
    }
}
