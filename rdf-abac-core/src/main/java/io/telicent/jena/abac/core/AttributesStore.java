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

import java.util.Set;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;

/**
 * API for getting the users attributes.
 * An Attribute store also has the attribute value hierarchies that apply.
 */
public interface AttributesStore extends HierarchyGetter {

    /*
     * Get attributes from this {@code AttributeStore} for a given user.
     * The user must not be null.
     * <ul>
     * <li>null {@literal ->} unknown user.
     * <li>empty AttributeSet {@literal ->} known user, no attributes.
     */
    public AttributeValueSet attributes(String user);

    /** Return a set of the users with an attribute value set in this store. */
    public Set<String> users();

    /** Does this attribute have an associated {@link Hierarchy hierarchy} of values? */
    public boolean hasHierarchy(Attribute attribute);

    /** Return the hierarchy for an attribute. Returns null if there isn't one. */
    @Override
    public Hierarchy getHierarchy(Attribute attribute);
}
