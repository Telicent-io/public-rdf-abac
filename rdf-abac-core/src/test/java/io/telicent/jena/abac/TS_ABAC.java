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

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
    TestAuthMisc.class

    // Component testing.
    , TestAttributeParser.class
    , TestAttributeExprList.class
    , TestAttributeExprParse.class
    , TestHierarchy.class

    , TestAttributeValue.class
    , TestAttributeValueList.class

    // Main test suite for attribute expression evaluation.
    , TestAttributeExprEval.class

    , TestLabelsStore.class
    , TestLabelMatch.class
    , TestLabels.class
    , TestAssemblerABAC.class
})

public class TS_ABAC {}
