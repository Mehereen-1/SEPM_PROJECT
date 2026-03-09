# Stage 1: Build the JAR
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the JAR, skip tests
RUN ./mvnw package -DskipTests -B

# Stage 2: Run the JAR
FROM eclipse-temurin:17-jdk-alpine

VOLUME /tmp

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]