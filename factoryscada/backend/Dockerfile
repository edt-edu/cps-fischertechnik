##################################
# Stage 1: Build the application
##################################
FROM gradle:8.10.0-jdk21 AS builder

# Set the working directory
WORKDIR /app

# Copy the Gradle wrapper and build files
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src ./src

# Build the application (no test)
RUN gradle assemble --no-daemon

##################################
# Stage 2: Package the application
##################################
FROM eclipse-temurin:21-jre AS runner

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port the app runs on
EXPOSE 9000

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--configuration.path=/app/config/configuration.yml", "--missions.configuration.path=/app/config/missions-configuration.yml"]
