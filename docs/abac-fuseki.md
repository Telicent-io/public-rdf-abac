# RDF ABAC: Fuseki Module

The `jena-fuseki-abac` module provides triple-level ABAC security for the 
[Apache Jena Fuseki](https://jena.apache.org/documentation/fuseki2/) triplestore.
Every triple is given a label that defines the access requirements and which must be
satisfied by the user/software in order to see the triple in query operations.

* [Fuseki Configuration](#fuseki-configuration)
  * [Service Configuration](#service-configuration)
  * [Dataset Configuration](#dataset-configuration)
  * [Example](#fuseki-configuration-example)
* [Fuseki Module for ABAC](#fuseki-module-for-abac)
  * [Installation](#installation)
  * [Authentication Setup](#authentication-setup)

## Fuseki Configuration

This section describes the ABAC authorization configuration.
This can be combined with [Fuseki API access control
](https://jena.apache.org/documentation/fuseki2/fuseki-data-access-control.html#acl) 
using ACLs.

### Service Configuration

There are two classes of operations: loading data and accessing data.

ABAC security applies to access operations, SPARQL query and SPARQL Graph Store Protocol read operations (HTTP GET).

Loading data and data labels is performed by an ABAC-specific service.
The endpoint must be protected by ensuring only trusted remote services can access
the data loading endpoint.

Example Fuseki service configuration:

```
PREFIX authz:   <http://telicent.io/security#>

:service1 rdf:type fuseki:Service ;
    fuseki:name "ds" ;

    fuseki:endpoint [ fuseki:operation authz:upload ; fuseki:name "upload" ] ;
    fuseki:endpoint [ fuseki:operation authz:query ; fuseki:name "sparql" ] ;
    fuseki:endpoint [ fuseki:operation authz:gsp-r ; fuseki:name "data" ] ;

    fuseki:dataset :dataset ;
    .
```

The operations `authz:query`, `authz:gsp-r` are the data access operations, and `auth:upload` 
is the data loading operation.

SPARQL Update is not currently supported.

The Fuseki module will also check for and rewrire the standard operations using the Fuseki namspace
that is using `fuseki:query` instead of `authz:query` if the dataset supports ABAC. This form must
be used if defining multiple opertions operations on a single endpoint and using Fuseki ability
to route based on introspection of the request.

```
:service1 rdf:type fuseki:Service ;
    fuseki:name "ds" ;

    fuseki:endpoint [ fuseki:operation fuseki:query ] ;
    fuseki:endpoint [ fuseki:operation fuseki:gsp-r ] ;
    fuseki:endpoint [ fuseki:operation authz:upload ; fuseki:name "upload" ] ;


### Dataset configuration

An ABAC supporting dataset is of type `authz:DatasetAuthz` and is stacked on top of another dataset
given by the `authz:dataset`.

See [RDF ABAC : User Attribute Store](abac-user-attribute-store.md) for details of
the local and remote attribute store options. An attribute store is required.

```
:dataset rdf:type authz:DatasetAuthz ;

    ## Dataset for the labels (required)
    authz:labels  :databaseLabels;

    ## API authorization by attribute (optional)
    authz:accessAttributes "engineer";

    ## Default policy when no triple label is found (optional)
    authz:tripleDefaultAttributes "!";

    ## One or other of these definitions for an attribute store.

    ## Attribute store
    authz:attributes <file:attribute-store.ttl> ;

    ## Remote Attribute store
    ## This is a URL template where {user} is replaced with the URL-safe encoding
    ## of the id of the user making a request.
    ## e.g. authz:attributesURL <http://otherHost/path?user={user}>
    ## e.g. authz:attributesURL <http://otherHost/lookup/{user}>
    ## e.g. authz:attributesURL <env:ATTRIBUTE_STORE> -- read URL from an environment variable.

    ## The data itself.
    authz:dataset :datasetBase;
    .
```

|||
|-----------------------------------|---------------------------|       
| _Access to the dataset_ | |
| `authz:accessAttributes`          | Additional API check based on request attributes |
| _Storage of the labels and patterns_ | |
| `authz:labels`                    | URL referring to the separate dataset storage of labels |
| _Default attribute_
| `authz:tripleDefaultAttributes`   | |
| _Attribute Store_
| `authz:attributes`                | Local attribute store (file)   |
| `authz:attributesURL`             | Remote attribute store access  |
| _Dataset_ | |
| `authz:dataset` | The underlying dataset |

If a triple is not given a label in a data upload, then the value of `authz:tripleDefaultAttributes`  is used.

### Fuseki Configuration Example

This is a complete configuration for a standalone deployment using a local attribute store.
```
PREFIX :        <#>
PREFIX fuseki:  <http://jena.apache.org/fuseki#>
PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ja:      <http://jena.hpl.hp.com/2005/11/Assembler#>
PREFIX tdb2:    <http://jena.apache.org/2016/tdb#>

PREFIX authz:   <http://telicent.io/security#>

:service rdf:type fuseki:Service ;
    fuseki:name "secured" ;

    fuseki:endpoint [ fuseki:operation authz:upload ; fuseki:name "upload" ] ;
    fuseki:endpoint [ fuseki:operation authz:query ; ] ;

    fuseki:dataset :dataset ;
    .

:dataset rdf:type authz:DatasetAuthz ;

    ## Default policy when no triple label is found.
    authz:tripleDefaultAttributes "!";

    ## Storage of labels.
    authz:labels  :databaseLabels;

    ## Attribute store
    authz:attributes <file:attribute-store.ttl> ;

    ## The data itself.
    authz:dataset :datasetBase;
    .

:datasetBase rdf:type tdb2:DatasetTDB2 ;
    ## Data on-disk
    tdb2:location "DB2" ;
    .

:databaseLabels rdf:type tdb2:DatasetTDB2 ;
    ## Labels on-disk
    tdb2:location "DB2-labels" ;
    .
```

This example show one service, with two endpoints:

* `http://_host_/secured/ `for SPARQL query access.
* `http://_host_/secured/upload` for labelled data upload.

Only one of `authz:attributes` and `authz:attributesURL` may be given.

`authz:attributes` refers to a 
[local user attribute store](abac-user-attribute-store.md#local-attribute-store)

`authz:attributesURL` refers to a 
[remote user attribute store](abac-user-attribute-store.md#remote-user-attributes-store)

## Fuseki Module for ABAC

The security engine provides a [Fuseki extension
module](https://jena.apache.org/documentation/fuseki2/fuseki-modules). When this
is on the classpath of the Fuseki server, the module is automatically enabled.

### Installation

Add to the classpath.
See [Fuseki Modules](https://jena.apache.org/documentation/fuseki2/fuseki-modules.html)

### Authentication Setup

Authentication of the request user is required before ABAC authorization can
be applied. Thie can be by bearer authentication (a separate Fuseki Module) 
or by features of Eclipse Jetty to perform
authentication, including integration with cloud and enterprise authentication
services. There is basic HTTP user/password authentication provided to simplify
testing and development.

### API Access

The module uses the servlet request principal as detemined by the
web container (Eclipse Jetty). This includes HTTP authentication. Jetty may be
configured for external authentication (OAuth, LDAP etc).

In addition, a dataset may have an additional attribute requirement for API
access. See `authz:accessAttributes`.
