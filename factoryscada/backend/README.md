# Fischertechnik SCADA

Welcome to the user manual for the backend factoryscada project. This document will guide you through the steps necessary to configure, build, and start the application.

## Prerequisites

Before you begin, ensure your environment is set up with the following:

- **Java Development Kit (JDK) 21 or higher**: You can download the appropriate version from the official [Oracle JDK](https://www.oracle.com/java/technologies/downloads/).
- **Gradle 8 or higher**: You can follow the installation instructions on the [Gradle](https://gradle.org/install/) website.
- **GitHub Packages credentials**: required to download the `sysml-mission-generator-gradle-plugin` (see below).

### GitHub Packages credentials

The build uses the `fr.inria.mbdo.mission.sysml-mission-generator-gradle-plugin` Gradle plugin, which is published on the GitHub Packages Maven registry of this repository (`https://maven.pkg.github.com/edt-edu/cps-fischertechnik`).
GitHub Packages requires authentication, **even for public packages**, so the build fails if no credentials are provided.

1. Create a GitHub **personal access token (classic)** with the `read:packages` scope
   (GitHub > Settings > Developer settings > Personal access tokens > Tokens (classic)).
   Fine-grained tokens are not supported by the GitHub Packages Maven registry.
2. Add the following lines to your **user** Gradle properties file `~/.gradle/gradle.properties`
   (on Windows: `%USERPROFILE%\.gradle\gradle.properties`). Do **not** put them in the project's `gradle.properties`, to avoid committing your token.
   ```properties
   gpr.user=<your-github-login>
   gpr.key=<your-personal-access-token>
   ```

Notes:

- If `gpr.user` / `gpr.key` are not defined, the build falls back to the `GITHUB_ACTOR` / `GITHUB_TOKEN` environment variables. This is what is used by the GitHub Actions CI, where both are provided automatically.
- The repositories are searched in this order (see [settings.gradle](settings.gradle)): `mavenLocal()`, then GitHub Packages, then the Gradle Plugin Portal. If you built and installed the plugin yourself (`publishToMavenLocal` in `sysml-mission-generator`), it is taken from `~/.m2/repository` and no credentials are needed.
- To diagnose credential problems, run for example:
  ```sh
  ./gradlew help --refresh-dependencies --info
  ```
  A `401` means missing or invalid credentials, a `403` means the token lacks the `read:packages` scope.

## Project Installation

1. **Clone the Repository**:
    - Open your terminal or command line.
    - Execute the following command to clone the project repository:
      ```sh
      git clone https://github.com/edt-edu/cps-fischertechnik.git
      ```
    - Change directory to enter the project folder:
      ```sh
      cd cps-fischertechnik/factoryscada/backend
      ```

2. **Import the Project into the IDE**:
    - Open your preferred IDE (IntelliJ IDEA, Eclipse, VSCode, etc.).
        - in some IDE, you need to install lombok (ex for eclipse: https://projectlombok.org/setup/eclipse)
    - Import the project as a Gradle project.

## Building the Project

To build the project, follow these steps:

1. **Run the Build Command**:
    - From the project's root directory, execute the following command:
      ```sh
      ./gradlew build
      ```
    - This command will compile the code, run the tests, and assemble the necessary artifacts.

2. **Deploy to local maven**

	
    - From the project's root directory, execute the following command:
      ```sh
      ./gradlew publishToMavenLocal
      ```
    - This command will copy the jar in your ~/.m2/repository for use by maven.

3. **Build the Docker image**

    - The Docker build also needs the GitHub Packages credentials. They are passed as BuildKit secrets (so they are not stored in the image) from the `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables:
      ```sh
      export GITHUB_ACTOR=<your-github-login>
      export GITHUB_TOKEN=<your-personal-access-token>
      docker build --secret id=gpr_user,env=GITHUB_ACTOR --secret id=gpr_token,env=GITHUB_TOKEN -t factoryscada-backend .
      ```

## Configuration

The application uses several small yaml based dsl for configuration.

The default configuration files are located in `src/main/resources`. 

- `command-placeholder.yml`  defines the  template json messages that can be sent to the controller. A template must exist for every command.
- `configuration.yml` defines the factory specific informations. List of PLC, connected machines, with their data (name, connection info, ...)


In production, `configuration.yml` can be replaced by your own using the following argument when calling the application `--configuration.path=/app/config/configuration.yml`.
You may also adjust spring configuration values by passing `--spring.config.additional-location=file:/app/config/configuration.yml` to the application. 

## Starting the Application

To start the application, you have two options:

1. **Using Gradle**:
    - Execute the following command:
      ```sh
      ./gradlew bootRun
      ```

2. **Using the JAR File**:
    - After building the project, execute the following command to start the application:
      ```sh
      java -jar build/libs/factoryscada-0.0.1-SNAPSHOT.jar
      ```


## Documentation

- [WebSocket protocol API](docgen/websocket-endpoints-topics.adoc) (backend to frontend communication)