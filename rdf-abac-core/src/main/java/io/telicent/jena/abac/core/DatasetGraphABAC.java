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

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphWrapper;
import org.apache.jena.sparql.core.Transactional;

public class DatasetGraphABAC extends DatasetGraphWrapper {
    // Attribute expression used to determine whether access is allowed.
    // Failing this test, the request is returns with 403 (Forbidden)
    // This may be null, meaning "no test applied".
    private final String accessAttributesStr;
    private final AttributeExpr accessAttributesExpr;
    private final LabelsStore labelsStore;
    // Default for when the labels store has no labels for a triple.
    // Can be null for system wide policy (which is deny).
    private final String defaultLabel;
    private final AttributesStore attributesStore;

    /** Return a {@code DatasetGraphAuthz} with empty labels, and no access attribute expression. */
    public static DatasetGraphABAC create(DatasetGraph dsg, String tripleDefaultAttributes, String datasetDefaultLabel, AttributesStore attributesStore) {
        String accessAttributes = null;
        LabelsStore labelsStore = Labels.createLabelsStore();
        return new DatasetGraphABAC(dsg, accessAttributes, labelsStore, datasetDefaultLabel, attributesStore);
    }

    private DatasetGraphABAC(DatasetGraph dsg, LabelsStore labelsStore, String datasetDefaultLabel, AttributesStore attributesStore) {
        this(dsg, null, labelsStore, datasetDefaultLabel, attributesStore);
    }

    public DatasetGraphABAC(DatasetGraph base, String accessAttributes,
                             LabelsStore labelsStore, String datasetDefaultLabel,
                             AttributesStore attributesStore) {
        super(base);
        this.accessAttributesStr = accessAttributes;
        this.accessAttributesExpr = AE.parseExpr(accessAttributesStr);
        this.labelsStore = labelsStore;
        this.defaultLabel = datasetDefaultLabel;
        this.attributesStore = attributesStore;
    }

    public AttributeExpr getAccessAttributes() {
        return accessAttributesExpr;
    }

    public String getDefaultLabel() {
        return defaultLabel;
    }

    public LabelsStore labelsStore() {
        return labelsStore;
    }

    /** Return the attribute store for this datasets. */
    public AttributesStore attributesStore() {
        return attributesStore ;
    }

    /** Return the function for getting the user's attributes for this datasets. */
    public AttributesForUser attributesForUser() {
        return attributesStore::attributes ;
    }

    // Propagate transactions to the labels store.

    private Transactional getOther() { return labelsStore.getTransactional(); }

    @Override
    public void begin() {
        getOther().begin();
        super.begin();
    }

    @Override
    public ReadWrite transactionMode() {
        getOther().transactionMode();
        return super.transactionMode();
    }

    @Override
    public TxnType transactionType() {
        getOther().transactionType();
        return super.transactionType();
    }

    @Override
    public void begin(TxnType type) {
        getOther().begin(type);
        super.begin(type);
    }

    @Override
    public void begin(ReadWrite readWrite) {
        getOther().begin(readWrite);
        super.begin(readWrite);
    }

    @Override
    public boolean promote() {
        getOther().promote();
        return super.promote();
    }

    @Override
    public boolean promote(Promote type) {
        getOther().promote(type);
        return super.promote(type);
    }

    @Override
    public void commit() {
        getOther().commit();
        super.commit();
    }

    @Override
    public void abort() {
        getOther().abort();
        super.abort();
    }

    @Override
    public void end() {
        getOther().end();
        super.end();
    }

    @Override
    public boolean isInTransaction() {
        getOther().isInTransaction();
        return super.isInTransaction();
    }

    @Override
    public boolean supportsTransactions() {
        // DatasetGraph operation.
        //getOther().supportsTransactions();
        return super.supportsTransactions();
    }

    @Override
    public boolean supportsTransactionAbort() {
        // DatasetGraph operation.
        //getOther().supportsTransactionAort();
        return super.supportsTransactionAbort();
    }
}
