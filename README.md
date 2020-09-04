# personify-scim-server

Lean, mean and high performant open source Spring Boot Java SCIM server implementation with pluggable persistence layer.

Usefull for exposing a company's identities using the SCIM protocol and target point for your provisioning engine., you only need to write the storage layer in JAVA.

You can reuse the integrated storage layer or write a custom java implementation and wire it via configuration.


## intro

basic server implementation.

**currently implemented :**

- create, get, put, delete, search, patch ( with attributes and excludedAttributes )
- bulk with circular reference processing, maxOperations and maxPayloadSize 
- paging
- filtering (all operators,and,sort)
- discovery
- schema validation
- uniqueness constraint validation
- authentication : basic and token ( with roles )
- token endpoint created and verified with forgerock IDM connector



**on the list :**

- filtering : complete specification
- /Me endpoint



##  

## usage

You can either choose to **download the binaries** or **clone the project** or **use the docker image**.

##  

For spinning up a scim server from the [downloaded binary](https://bitbucket.org/wouter29/personify-scim-server/downloads/) : 

> java -jar -Dserver.port=8080 personify-scim-server-1.1.1.RELEASE.jar

When port 8080 is already taken or other problems occur, adapt the server.port via the commandline.

SSL can also be configured this way ( see spring-boot documentation and sample in application.properties for this ).

##    

When you **clone** the project, you can build the binary from source.

Requirements to build are installation of a Java JDK 1.8+ and Maven.

For running the maven project :

> mvn spring-boot:run

##  

Use the integrated [postman collection](https://bitbucket.org/wouter29/personify-scim-server/src/master/scim.postman_collection.json) to test.

##  

A **load test** is also runnable :

##  

```
mvn exec:java -Dexec.mainClass=be.personify.iam.scim.util.LoadTest -Dexec.args="http://localhost:8080/scim/v2 scim-user changeit 4 2000"
[INFO] --- exec-maven-plugin:1.1.1:java (default-cli) @ personify-scim-server ---
starting load test to http://localhost:8080/scim/v2 with 4 threads
thread [3] 2000 records processed in 11108
thread [1] 2000 records processed in 11486
thread [2] 2000 records processed in 11599
thread [0] 2000 records processed in 11634
8000 records processed in 11737 ms
loadTestCreate() 727 per second
thread [3] 2000 records processed in 14089
thread [2] 2000 records processed in 14191
thread [0] 2000 records processed in 14216
thread [1] 2000 records processed in 14222
8000 records processed in 14318 ms
loadTestDelete() 571 per second
user created with id b18fd14b-3d68-4dd7-a22a-b0434c1f755c
thread [0] 2000 records processed in 5322
thread [1] 2000 records processed in 5331
thread [3] 2000 records processed in 5380
thread [2] 2000 records processed in 5436
8000 records processed in 5519 ms
loadTestGet() 1600 per second
```


##   

If you **really** do not want to build anything : spin up the [docker image](https://hub.docker.com/r/personify/personify-scim-server)
> docker run -p 8080:8080 personify/personify-scim-server:1.1.1.RELEASE
 
##   

If you **really really** do not want to build or run it yourself : point the [postman collection ](https://bitbucket.org/wouter29/personify-scim-server/src/master/scim.postman_collection.json)
towards https://www.personify.be/scim/v2/Users ( 401 means unauthorized, so use correct basic auth credentials : scim-user/changeit )
create a environment in postman containing host and protocol.

##  

## configuration

Pimp the application.properties file included.

The storage implementation class can be changed to the one you implemented.

* memory storage implementation : basic fast access with flushing to a file
* mongo storage implementation

For using a OAUTH bearer token on forgerock openidm and other products, just point the endpoint to http://localhost:{8090}/scim/v2/token and use the credentials from the application.properties.

Custom schemas can also be configured simply by defining it in a file.



##  

## issues

if you find any issues or have enhancement requests, please [create a issue](https://bitbucket.org/wouter29/personify-scim-server/issues/new)
it will be looked at.


##

## thanks

* jingzhou wang : logging various issues and writing the mongo storage implementation




