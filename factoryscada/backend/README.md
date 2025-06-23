# Fischertechnik SCADA

Welcome to the user manual for the backend factoryscada project. This document will guide you through the steps necessary to configure, build, and start the application.

## Prerequisites

Before you begin, ensure your environment is set up with the following:

- **Java Development Kit (JDK) 17 or higher**: You can download the appropriate version from the official [Oracle JDK](https://www.oracle.com/java/technologies/downloads/).
- **Gradle 8 or higher**: You can follow the installation instructions on the [Gradle](https://gradle.org/install/) website.

## Project Installation

1. **Clone the Repository**:
    - Open your terminal or command line.
    - Execute the following command to clone the project repository:
      ```sh
      git clone https://git.rwth-aachen.de/mbdo/mbdo-impl.git
      ```
    - Change directory to enter the project folder:
      ```sh
      cd mbdo-impl/REN_Setups/physical-impl/factoryscada/backend
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

## Configuration

The application uses several small yaml based dsl for configuration.

The default configuration files are located in `src/main/resources`. 

- `command-placeholder.yml`  defines the  template json messages that can be sent to the controller. A template must exist for every command.
- `configuration.yml` defines the factory specific informations. List of PLC, connected machines, with their data (name, connection info, ...)


In production, `configuration.yml` can be replaced by your own using the following argument when calling the application `--configuration.path=/app/config/configuration.yml`

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