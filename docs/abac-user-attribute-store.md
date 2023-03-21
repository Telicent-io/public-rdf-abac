# RDF ABAC : User Attribute Store

The attributes (permissions) for each user are held in a user attribute store.
This can be a local store, for when running RDF ABAC as a standalone service or
for a development system,
or a separate service that may be shared, and the ABAC engine needs a URL to
fetch the user attributes.

The attribute store also stores the attribute value hierarchies.

### Local Attribute Store

For testing and development, and for a standalone deployment, there is a simple,
file-based local attribute store. It is specified in the dataset description with
`authz:attributes`. It is an RDF file, with `authz:user` and `authz:userAttribute`
properties. Users have specific attibutes, not attribute expressions.

A local attribute store is loaded when the Fuseki server starts up and 
does not change while the server is running.

```
PREFIX authz: <http://telicent.io/security#>

[] authz:user "u1" ;
    authz:userAttribute "manager";
    authz:userAttribute "project-Y";
    .
    
[] authz:user "u2" ;
    authz:userAttribute "engineer";
    .

[] authz:user "u3" ;
    authz:userAttribute "unused";
    .
```

### Remote User Attributes Store

An attribute store may be remote. Attributes needed for the request are fetched.  
The configuration needs to include the URL template.

```
        authz:attributesURL` "https://someHost/user/{user}"
```

or the value may be the special form to read the URL template from an environment variable:

`<env:ENV_VAR>`

The URL is used by replacing "{user}" with the user name from the request.

For example: for user _Smith_ "https://someHost/user/{user}" becomes "https://someHost/users/Smith"

```
:dataset rdf:type authz:DatasetAuthz ;
    ## Attribute store
    authz:attributesURL <env:USER_ATTRIBUTE_STORE>;
```
where environment variable `USER_ATTRIBUTE_STORE` has the value
`https://host/user/{user}` is equivalent to:
```
:dataset rdf:type authz:DatasetAuthz ;
    ## Attribute store
    authz:attributesURL <https://host/user/{user}>;
    ...
```
except the URL is not hardcoded into the configuration file.
