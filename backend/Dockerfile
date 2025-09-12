FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy maven files
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw package -DskipTests

# Run application
EXPOSE 8080
CMD ["java", "-jar", "target/sand-dollar-backend-1.0.0.jar"]