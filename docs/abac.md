# Attribute-Based, Triple-level security.

* Overview (this document)
* [Description](abac-description.md)
* [Specification: RDF-ABAC label syntax, transport and evaluation](abac-specification.md)
* [User Attribute Store](abac-user-attribute-store.md)
* [Attribute Value Hierarchies](abac-hierarchies.md)
* [Fuseki ABAC Module](abac-fuseki.md)
* [Attribute Label Evaluation service (ALE)](abac-label-eval-service.md)

This component module provides security labelling of RDF data. 
Each data triple has an associated security label; the label
gives a condition for access to be granted and is tested
during every request.

Access requests have a set of attribute-values
that are the access rights of the user or software making the request.
We usually just refer to "the user".  This builds on strong user
authentication and a secure user attribute rights service.

By filtering for security at the storage level of the system, 
we ensure all data is test for visiblity,
whether accesed by query or retrieved for processing elsewhere.

The component consists of a ABAC security engine,
an extension module for [Apache Jena Fuseki](https://jena.apache.org/documentation/fuseki2/),
and a security evaluation service to provide security verification
to non-JVM components of the system.

### Attribute-Based Acccess Control (ABAC)

Security requirements for access to an item data are expressed with an
attribute expression.

A simple example is:
```
    employee
```
which requires the request to have `employee` attribute.

More complex expressions are possible: 
```
    employee | ( contractor & nationality=UK )
```
which requires that the request
has the `employee` attribute or the attribute `contract` together with 
the `nationality` attribute with value "UK".

Hierarchies of classifications are supported. Suppose a hierarchy of 
"public" (least restrictive), "confidental", "sensitive", and 
"private" (most restricive) is defined for attribute `classification`.
If a request carries  "classification=sensitive" then the request has access to
data labelled "confidental" and "public" as well, and would match for data
condition of `classification = confidental`.
