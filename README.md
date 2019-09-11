# personify-scim-server

Spring Boot Java SCIM server implementation with pluggable persistence layer

## intro

basic server implementation.

**currently implemented :**

- create, get, put, delete, search
- discovery
- basic authentication
- schema validation
- paging
- uniqueness constraint validation


**on the list :**

- bulk
- filtering
- patch
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


## configuration

pimp the application.properties file included

