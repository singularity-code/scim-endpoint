# personify-scim-server

Spring Boot Java SCIM server implementation with pluggable persistence layer

## intro

basic server implementation.

**currently implemented :**

- create, get, put, delete, search, patch
- discovery
- basic authentication
- schema validation
- paging
- uniqueness constraint validation
- bulk ( ongoing )

**on the list :**

- filtering
- JWT



## usage

You can either choose to download the binaries or clone the project.

For spinning up a scim server from the [downloaded binary](https://bitbucket.org/wouter29/personify-scim-server/downloads/) : 

> java -jar personify-scim-server-1.0.0-SNAPSHOT.jar

When you have cloned the project, you can build the binary from source.

Requirements to build are installation of a Java JDK 1.8+ and Maven.

For running the maven project :

> mvn spring-boot:run


use the integrated [postman collection](https://bitbucket.org/wouter29/personify-scim-server/src/master/scim.postman_collection.json) to test.

a storage implementation is included, tune or implement other storages

if you **really** do not want to build or run it yourself : point the postman collection 
towards https://www.personify.be/scim/v2/Users

if you **really really** do not want to build anything : spin up the [docker image](https://hub.docker.com/r/personify/personify-scim-server)

## configuration

pimp the application.properties file included

