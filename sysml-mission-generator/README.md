# sysml-mission-generator

## Overview
`sysml-mission-generator` is a **Maven multi‑module project** that allows to generate Java source code from SysML model files.  
The generated code is primarily intended to implement dynamic missions for the **Fischertechnik factory** reference implementation but can probably be used for other purposes.

The generated code relies on a small runtime library.

The project provides **3 user front‑ends**:

| Front‑end | Description |
|-----------|--------------|
| **CLI** (`sysml-mission-generator-cli`) | Stand‑alone command‑line tool for ad‑hoc generation. |
| **Maven plugin** (`sysml-mission-generator-maven-plugin`) | Seamlessly integrates code generation into a Maven build lifecycle (`generate-sources` phase). |
| **Gradle plugin** (`sysml-mission-generator-gradle-plugin`) | Same functionality for Gradle builds (`generateSources` task). |

A **runtime library** (`sysml-mission-runtime`) supplies the APIs that the generated code depends on.  

A set of ready‑made SysML models for the Fischertechnik factory is shipped in the `missions-design-models` folder.

## Project Structure

```
sysml-mission-generator/
├─ README.md                     ← This file
├─ pom.xml                       ← Top‑level aggregator POM
├─ missions-design-models/       ← SysML model files (CB, MPS, SL, VGR …)
├─ sysml-mission-generator/      ← Core generator implementation
├─ sysml-mission-generator-cli/  ← CLI front‑end
├─ sysml-mission-generator-maven-plugin/ ← Maven plugin
├─ sysml-mission-generator-gradle-plugin/← Gradle plugin
└─ sysml-mission-runtime/       ← Runtime library required by generated code
```

Each sub‑module is a standard Maven module with its own `pom.xml`.

## Building the Project

From the project root:

```bash
mvn clean install
```

All modules are compiled, tested, and installed into your local Maven repository.  
The generated JARs can be found in the `target/` directory of each module.


> **Tip:** optionnaly add `-DskipTests -Dinvoker.skip=true` to avoid running all the tests and integration tests

## Using the generator

Look into you preferred front end README.md for detailled instructions:
* [Command line interface](sysml-mission-generator-cli)
* [maven plugin](sysml-mission-generator-maven-plugin)
* [gradle plugin](sysml-mission-generator-gradle-plugin)


## Runtime Library

All generated code depends on classes from **`sysml-mission-runtime`** (e.g., `MissionExecutor`, `Task`, `Sensor` abstractions).  
Add this library as a dependency to any project that consumes the generated sources.

## SysML Model Repository

The `missions-design-models` module contains a set of ready‑to‑use mission specifications that model the reference Fischertechnik factory dynamic mission:

* `CB/` – Conveyor‑belt‑based missions  
* `MPS/` – Multi‑process‑system missions  
* `SL/` – Simple line missions  
* `VGR/` – Vehicle‑guided‑robot missions  
* `common/` – Shared definitions (parts, sensors)  
* `zones/` – Zone‑level layout models  

These models are used in the modules tests.

In your projects, feel free to replace these models with your own SysML files; the generator works with any valid `.sysml` model.

> **Important:** The load order of the files is important for correct processing. Cross references between files are not supported.


---

This project is licensed under the **Apache License, Version 2.0**. See the `LICENSE` file for full details.

---

