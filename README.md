# personify-scim-server

Open source Spring Boot Java SCIM server implementation with pluggable persistence layer.

If you need to expose your identities using the SCIM protocol, you only need to write the storage layer in JAVA.

## intro

basic server implementation.

**currently implemented :**

- create, get, put, delete, search, patch
- discovery
- basic authentication
- schema validation
- paging
- uniqueness constraint validation
- bulk
- simple filtering : basic operators, and, sort
- roles (read/write) on basic authentication

**on the list :**

- filtering : complete specification
- JWT


##  

## usage

You can either choose to **download the binaries or clone the project**.

##  

For spinning up a scim server from the [downloaded binary](https://bitbucket.org/wouter29/personify-scim-server/downloads/) : 

> java -jar personify-scim-server-1.0.9.RELEASE.jar

When port 8080 is already taken or other problems occur, edit the jar -> find application.properties and adapt the server.port or other settings.

SSL can also be configured this way ( see spring-boot documentation and sample in application.properties for this ).

##    

When you **clone** the project, you can build the binary from source.

Requirements to build are installation of a Java JDK 1.8+ and Maven.

For running the maven project :

> mvn spring-boot:run



use the integrated [postman collection](https://bitbucket.org/wouter29/personify-scim-server/src/master/scim.postman_collection.json) to test.

a storage implementation is included, tune or implement other storages

##   

if you **really** do not want to build anything : spin up the [docker image](https://hub.docker.com/r/personify/personify-scim-server)
> docker run -p 8080:8080 personify/personify-scim-server:1.0.9.RELEASE
 
##   

if you **really really** do not want to build or run it yourself : point the [postman collection ](https://bitbucket.org/wouter29/personify-scim-server/src/master/scim.postman_collection.json)
towards https://www.personify.be/scim/v2/Users ( 401 means unauthorized, so use correct basic auth credentials : scim-user/changeit )
create a environment in postman containing host and protocol.

##  

## configuration

pimp the application.properties file included.
the storage implementation class can be changed to the one you implemented.





##  

## issues

if you find any issues or have enhancement requests, please [create a issue](https://bitbucket.org/wouter29/personify-scim-server/issues/new)
it will be looked at.



