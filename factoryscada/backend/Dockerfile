# syntax=docker/dockerfile:1
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
# GitHub Packages credentials are needed to resolve the sysml-mission-generator plugin.
# They are passed as BuildKit secrets so they never end up in an image layer:
#   docker build --secret id=gpr_user,env=GITHUB_ACTOR --secret id=gpr_token,env=GITHUB_TOKEN .
RUN --mount=type=secret,id=gpr_user,env=GITHUB_ACTOR \
    --mount=type=secret,id=gpr_token,env=GITHUB_TOKEN \
    gradle assemble --no-daemon

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
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--configuration.path=/app/config/configuration.yml", "--missions.configuration.path=/app/config/missions-configuration.yml", "--missions.parallelized.configuration.path=/app/config/missions-parallelized-configuration.yml"]
