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

import org.apache.jena.atlas.logging.LogCtl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Parser tests - run with no warning or errors logged.
 */
public class AbstractParserTests {
    protected static String level = null;
    @BeforeAll
    public static void beforeAll() {
        level = LogCtl.getLevel(ABAC.AttrLOG);
        LogCtl.disable(ABAC.AttrLOG);
    }

    @AfterAll
    public static void afterAll() {
        LogCtl.setLevel(ABAC.AttrLOG, level);
    }
}
