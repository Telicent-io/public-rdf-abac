# ABAC - Attribute-Based Access Control

ABAC provides access control on data. Each data item has an associated attribute
expression and a data item is only visible to the application if the attribute
expression evaluates to "true".  The attributes are evaluated in the context of a data access
request (query); the user has a number of attributes which represent the
permissions 

The associated attribute expression maybe a default for the dataset.

For example: the data label `"engineer"` might mean that the user making the data access
request (query) and if the user has the attributes `"engineer"` and `"employee"`
that are allowed to access the data item.

Documentation: [docs/abac](./docs/abac.md)

## Build

Run
```
   mvn clean install
```

which creates the `rdf-abac-fmod` modulke for Fuseki.

## Running Fuseki

See "[Configuring Fuseki](https://jena.apache.org/documentation/fuseki2/fuseki-configuration.html)"
for authentication.

This project uses the Apache Jena Fuseki Main server and is configured with a
Fuseki configuration file.

The jar is `rdf-abac-fmod/target/rdf-abac-fmod-VER.jar`

Get a copy of Fuseki Main:

```
wget https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/4.7.0/jena-fuseki-server-4.7.0.jar
```

Get a copy of the script
[fuseki-main](https://github.com/Telicent-io/jena-fuseki-kafka/blob/main/fuseki-main).

Place the jar in a directory `lib/` then run:

```
fuseki-main jena-fuseki-server-4.7.0.jar --conf config.ttl`
```

where `config.ttl is the configuration file for the server including the
connector setup.

### Release

This assumes you agve access to a release repository configured for maven.

On branch `main`:

Edit and commit `release-setup` to set the correct versions.

```
source release-setup
```
This prints the dry-run command.

If you have run this file, then change it, simply source the file again.

Dry run 
```
mvn $MVN_ARGS -DdryRun=true release:clean release:prepare
```

and for real

```
mvn $MVN_ARGS release:clean release:prepare
mvn $MVN_ARGS release:perform
```

This updates the version number.

After release, do `git pull` to sync local and remote git repositories.
