# personify-scim-server

Lean, mean and high performant open source Spring Boot Java SCIM server implementation with pluggable persistence layer.

Usefull for exposing a company's identities using the SCIM protocol and target point for your provisioning engine.

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
- authentication : basic and OAUTH bearer token ( with roles )
- bearer token endpoint created and verified with forgerock IDM connector



**on the list :**

- filtering : complete specification
- /Me endpoint



##  

## usage

You can either choose to **download the binaries** or **clone the project** or **use the docker image**.

## 

Use the integrated [postman collection](https://bitbucket.org/wouter29/personify-scim-server/src/master/scim.postman_collection.json) to test.


##  

For spinning up a scim server from the **[downloaded binary](https://bitbucket.org/wouter29/personify-scim-server/downloads/)**: 

> java -jar -Dserver.port=8080 personify-scim-server-1.1.2.RELEASE.jar

When port 8080 is already taken or other problems occur, adapt the server.port via the commandline.

SSL can also be configured this way ( see spring-boot documentation and sample in application.properties for this ).

##    

When you **clone** the project, you can build the binary from source.

Requirements to build are installation of a Java JDK 1.8+ and Maven.

For building :

> mvn clean install -Dgpg.skip


For running the maven project :

> mvn spring-boot:run

##  
 

If you **really** do not want to build anything : spin up the **[docker image](https://hub.docker.com/r/personify/personify-scim-server)**
> docker run -p 8080:8080 personify/personify-scim-server:1.1.2.RELEASE

Or integrate it into your cloud environment.

Different environment variables can be used to choose the storage implementation, configure the connections and tune the behaviour.

In your docker container specify/override environment entries you find in [application.properties](https://bitbucket.org/wouter29/personify-scim-server/src/master/src/main/resources/application.properties)

>docker run -p 8080:8080 -e scim.storage.implementation=... -e scim.storage.mongo.database=users personify/personify-scim-server:1.1.2.RELEASE

 
##   

If you **really really** do not want to build or run it yourself : point the [postman collection ](https://bitbucket.org/wouter29/personify-scim-server/src/master/scim.postman_collection.json)
towards https://www.personify.be/scim/v2/Users 

401 means unauthorized, so use correct basic auth credentials : scim-user/changeit

create a environment in postman containing host and protocol and import the collection.

##  

##  

## configuration

Pimp the application.properties file included.


Three storage implementations are developed but if needed, you can implement and wire your own implementation (see again in [application.properties](https://bitbucket.org/wouter29/personify-scim-server/src/master/src/main/resources/application.properties)).

* memory storage implementation : basic fast access with flushing to a file, choose this for development or show case, demo testing, trial.
* mongo storage implementation : should be fine and scalable for development and production.
* ldap storage implementation (experimental) : tested with ForgedRock Directory Server 6.5, consider it as a easy **scim to ldap**.

For using a OAUTH bearer token on forgerock openidm and other products, just point the endpoint to http://localhost:{8090}/scim/v2/token and use the credentials from the application.properties.

Custom schemas can also be configured simply by defining it in a file. Not tested yet, but give it a whirl and throw some feedback!


A **load test** is also runnable, below the response of a single instance :

##  

```
>mvn exec:java -Dexec.mainClass=be.personify.iam.scim.util.LoadTest -Dexec.args="http://localhost:8080/scim/v2 scim-user changeit 4 5000"
...
starting load test to http://localhost:8080/scim/v2 with 4 threads and 5000 requests....
thread [1] 5000 records processed in 22377
thread [0] 5000 records processed in 22499
thread [2] 5000 records processed in 22542
thread [3] 5000 records processed in 22545
20000 records processed in 22658 ms
--------------- loadTestCreate() --- 882.69 req/sec
thread [3] 5000 records processed in 12351
thread [0] 5000 records processed in 12392
thread [1] 5000 records processed in 12418
thread [2] 5000 records processed in 12426
20000 records processed in 12428 ms
--------------- loadTestGet()    --- 1609.27 req/sec
thread [1] 5000 records processed in 124684
thread [2] 5000 records processed in 124864
thread [3] 5000 records processed in 125226
thread [0] 5000 records processed in 125238
20000 records processed in 125241 ms
--------------- loadTestSearch()    --- 159.69 req/sec
thread [1] 5000 records processed in 18547
thread [2] 5000 records processed in 18862
thread [0] 5000 records processed in 19441
thread [3] 5000 records processed in 20076
20000 records processed in 20126 ms
--------------- loadTestDelete() --- 993.74 req/sec

```
##  

The current benchmark can give you already an idea about the throughput.

Executed on a single AMD® Ryzen 3 2200g with the application consuming approximately 250MB for 4 threads and 5000 requests per thread.

| request | MEM/FILE    | MONGO      | LDAP (FR DS) |
|---------|-------------|------------|--------------|
| create  | 882  req/s  | 225  req/s | 497  req/sec |
| get     | 1609 req/s  | 1997 req/s | 1056 req/sec |
| search  | 159  req/s  | 1446 req/s | ?            |
| delete  | 993  req/s  | 2015 req/s | 756  req/sec |


Mongo seems the fastest, although that the scaling and replication capabilites of the Forgerock Directory Server were not really utilized to the full extent.  

Creation of objects is a weak point of the MongoDB compared to the others, probably the reason of the fast read.


##  


##  

## issues

if you find any issues or have enhancement requests, please [create a issue](https://bitbucket.org/wouter29/personify-scim-server/issues/new)
it will be looked at.


##

##  

## thanks

feel free to contribute, we can all make life more easy for each other and SCIM on!


* jingzhou wang : logging various issues and writing the mongo storage implementation


##  

##  



