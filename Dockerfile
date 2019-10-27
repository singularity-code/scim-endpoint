
FROM openjdk:8-jdk-alpine
VOLUME /tmp
VOLUME /export/data/logs

RUN apk --no-cache add curl
RUN apk --no-cache add bash

ARG JAR_FILE
COPY target/${JAR_FILE} /app.jar

ENTRYPOINT ["java","-jar","/app.jar"]

EXPOSE 8080
