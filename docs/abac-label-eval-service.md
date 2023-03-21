# RDF ABAC: Label Evaluation Service

The ABAC security engine is written in Java.

Not all smart caches are written in Java.

The Attribute Evalutator service provides an (HTTP|gRPC) API
to the security engine with one operation:

```
    (user, security label) -> yes or no
```

A smart cache can call this service to get label evaluation. This makes writing
Smart Caches in non-Java languages easier and quicker - there does not have to
be a label parser or label evaluator for every language, nor user attribute handling.

As this is being applied to fine grain data items (triples, for the
graph smart cache; parts of document for search; location records for geo),
evaluation needs to be fast at scale. But we also want the
labelling to travel with the data which means it is a string expression.

We expect data to have repeated labels because a single source will
typically be labelling all its event in the same way.

A local cache in front of the API requests to the evaluator service will have a high hit rate.

This cache is must be sensitive to the user making the request
It may be per-request or per user session to ensure that changes to user
attributes are reflected quickly through out the system.  At most, a short
timeout on the cache lifetime after the request has been processed ("short"
means a few seconds) could be used.

A beneficial consequence of this service is that user attributes are not passed
to the smart-cache itself.

The ABAC Label Evaluator (ALE) is part of the secure layer of Telicent
CORE. Like Kafka, and smart-caches, it is a service that other services can rely
on.
