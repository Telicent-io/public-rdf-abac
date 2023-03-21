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

package io.telicent.jena.abac.core;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;

/** Mock attribute set manager */
public class AttributesStoreLocal implements AttributesStoreModifiable {

    private Map<String, AttributeValueSet> attributeRegistry = new ConcurrentHashMap<>();
    private Map<Attribute, Hierarchy> hierarchyRegistry = new ConcurrentHashMap<>();

    public AttributesStoreLocal() {}

    @Override
    public Set<String> users() {
        return Set.copyOf(attributeRegistry.keySet());
    }

    @Override
    public void put(String user, AttributeValueSet attributeSet) {
        attributeRegistry.put(user, attributeSet);
    }

    @Override
    public void clear() {
        attributeRegistry.clear();
    }

    @Override
    public AttributeValueSet attributes(String user) {
        Objects.requireNonNull(user);
        AttributeValueSet attrSet = attributeRegistry.get(user);
        return attrSet;
    }

    @Override
    public boolean hasHierarchy(Attribute attribute) {
        return hierarchyRegistry.containsKey(attribute);
    }

    @Override
    public Hierarchy getHierarchy(Attribute attribute) {
        return hierarchyRegistry.get(attribute);
    }

    @Override
    public void addHierarchy(Hierarchy hierarchy) {
        hierarchyRegistry.put(hierarchy.attribute(), hierarchy);
    }
}
