# personify-scim-server

Spring Boot SCIM server implementation with pluggable persistence layer

## intro

for testing clients, this basic server implementation can be used.
with a little bit of tuning and extra development, it can become production ready.

currently implemented :

- create, get, put, delete, search
- discovery
- basic authentication
- schema validation

on the list :

- bulk
- filtering
- paging



## usage

You can either choose to download the binaries or clone the project.

For spinning up a scim server from the [downloaded binary](https://bitbucket.org/wouter29/personify-scim-server/downloads/) : 

> java -jar personify-scim-server-1.0.0-SNAPSHOT.jar

After cloning the project, you can build the application from source.
Requirements to build are installation of a Java JDK 1.8+ and Maven.

For running the maven project :

> mvn spring-boot:run





use the integrated postman collection to test.

a storage implementation is included, tune or implement other storages
