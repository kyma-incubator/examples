FROM openjdk:8
MAINTAINER Andreas Krause <and.krause@sap.com>
ENV version=0.1
RUN mkdir -p /usr/java/dummy 
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/java/service/service.jar

# COPY target/ContainerDemoSimple-0.0.1-SNAPSHOT.jar /usr/java/service/service.jar
EXPOSE 8080
CMD ["java", "-jar", "/usr/java/service/service.jar"]