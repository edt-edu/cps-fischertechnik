# sysml‑mission‑generator‑cli

## Overview
`sysml‑mission‑generator‑cli` is a standalone command‑line tool that generates Java source code for missions from one or more SysML model files.  
The tool is packaged as a fat JAR (`sysml‑mission‑generator‑cli-1.0.0‑SNAPSHOT‑standalone.jar`) that can be run on any system with Java 21 or newer.


## Building the CLI (optional)

The command

```bash
cd sysml-mission-generator-cli
mvn clean package   
```
produces `target/sysml-mission-generator-cli-1.0.0-SNAPSHOT-standalone.jar` (recommanded version as it includes all its dependencies and is able to run standalone) and `target/sysml-mission-generator-cli-1.0.0-SNAPSHOT.jar` (wihtout dependencies that may me embedded in other frontend build).


However, most changes usually occurs in the `sysml-mission-generator` module, sot the recommanded command is to run `mvn clean package` in the parent folder.

## Running the tool

```bash
java -jar target/sysml-mission-generator-cli-1.0.0-SNAPSHOT-standalone.jar -h
```

Displays the help message:

```
Usage: SysmlMissionGenerator [-h] -o=<targetFolderPath> -p=<basePackageName>
                             -i=<sysmlFilePaths>... [-i=<sysmlFilePaths>...]...
Generates missions java code from one or more SysML files.
  -h, --help   Display this help message
  -i, --input=<sysmlFilePaths>...
               Path(s) to SysML file(s)
  -o, --output=<targetFolderPath>
               Path to output folder
  -p, --package=<basePackageName>
               base package name
```

### Example invocation

```bash
java -jar target/sysml-mission-generator-cli-1.0.0-SNAPSHOT-standalone.jar \
     -i ../missions-design-models/common/common_def.sysml \
     -i ../missions-design-models/CB/cb_def.sysml \
     -i ../missions-design-models/VGR/vgr_def.sysml \
     -i ../missions-design-models/CB/cb_missions_def.sysml \
     -p com.example.missions \
     -o generated-sources/
```

- **`-i` / `--input`** – one or more SysML files (you can repeat the flag).  
- **`-p` / `--package`** – the base Java package for the generated classes.  
- **`-o` / `--output`** – directory where the generated source tree will be created.

After the command finishes, the `generated-sources/` folder will contain the java files implementing the SysML model files structure and behavior.

## Adding the generated code to a project
The generated sources depend on the runtime library (`sysml-mission-runtime`). Add it as a dependency in your build tool (Maven, Gradle, etc.) and compile the generated code along with your own sources.