# personify-scim-server

Spring Boot SCIM server implementation with pluggable persistence layer

## intro

for testing clients, this basic server implementation can be used.<br/>
with a little bit of tuning and extra development, it can become production ready.<br/>

currently implemented :

- create, get, put, delete, search
- discovery

on the list :

- bulk
- filtering
- paging



## usage

for running the maven project : <br/>

> mvn spring-boot:run

for running the <a href="https://bitbucket.org/wouter29/personify-scim-server/downloads/">downloaded artefact</a> : <br/>

> java -jar personify-scim-server-1.0.0-SNAPSHOT.jar

use the integrated postman collection to test.

a storage implementation is included, tune or implement other storages
