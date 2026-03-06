# Use JDK 17 Alpine
FROM eclipse-temurin:17-jdk-alpine

# Create temp volume
VOLUME /tmp

# Copy the built Spring Boot jar into the container
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Run the jar
ENTRYPOINT ["java","-jar","/app.jar"]