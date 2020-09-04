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

A **load test** is also runnable, below the response of a single instance :

##  

```
>mvn exec:java -Dexec.mainClass=be.personify.iam.scim.util.LoadTest -Dexec.args="http://localhost:8080/scim/v2 scim-user changeit 4 5000"
...
[INFO] --- exec-maven-plugin:1.1.1:java (default-cli) @ personify-scim-server ---
starting load test to http://localhost:8080/scim/v2 with 4 threads and 5000 requests....
thread [1] 5000 records processed in 21617
thread [2] 5000 records processed in 21723
thread [0] 5000 records processed in 21778
thread [3] 5000 records processed in 21855
20000 records processed in 22032 ms
--------------- loadTestCreate() --- 907.77 req/sec
thread [2] 5000 records processed in 10968
thread [0] 5000 records processed in 11044
thread [1] 5000 records processed in 11052
thread [3] 5000 records processed in 11184
20000 records processed in 11213 ms
--------------- loadTestGet()    --- 1783.64 req/sec
thread [1] 5000 records processed in 147731
thread [0] 5000 records processed in 148067
thread [3] 5000 records processed in 148258
thread [2] 5000 records processed in 148718
20000 records processed in 148734 ms
--------------- loadTestDelete() --- 134.47 req/sec
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


Two storage implementations are developed but if neede, you can implemented and wire your own implementation.

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




