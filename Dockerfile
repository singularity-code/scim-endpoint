
FROM openjdk:8-jdk-alpine
VOLUME /tmp

ARG JAR_FILE

ARG MEMORY_IMPL=be.personify.iam.scim.storage.impl.MemoryStorage

ENV scim.storage.implementation $MEMORY_IMPL

COPY target/${JAR_FILE} /app.jar

ENTRYPOINT ["java","-jar","/app.jar"]

EXPOSE 8080
