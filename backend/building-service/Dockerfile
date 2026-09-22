FROM eclipse-temurin:17-jre-jammy@sha256:e85989f3e4d136b3d7dde921e157fddb9c7016805a225c1ec483326b825b3ca5
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY target/building-service-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
