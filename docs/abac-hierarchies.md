# RDF-ABAC : Attribute Value Hierarchies

Attribute Value Hierarchies provide a way to grant a user an attrbute value permission
that has teh idea that it also grants lesser permisison.

Hierarchies are defined and managed by the [User Attribute Store](abac-user-attribute-store.md).

Suppse we have the hierarchy for the attribute `status`:

```
[] authz:hierarchy [ authz:attribute "status" ;
                     authz:attributeValues "public, confidential, sensitive, private" ];
```
The list is writtern in least-most restrictive order.
"public" is the least restrictive, "private" the most restrictive.


If the data triple has the label
```
    status=public
```

then a user with attribute value `status=confidental` can see that data triple.
