# Use the official OpenJDK 21 image as the base image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the target directory to the container
COPY target/user-authentication-service-jwt-api-1.0.0.jar  app.jar

# Expose the port your Spring Boot app listens on (default: 8080)
EXPOSE 8080

# Set the command to run the application
CMD ["java", "-jar", "app.jar"]
