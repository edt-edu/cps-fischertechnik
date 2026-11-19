**Proposed README.md for `sysml-mission-generator-plugin`**

---

# SysML Mission Generator Gradle Plugin  

A Gradle plugin that makes the **SysML Mission Generator** usable from a standard Java/Scala/Kotlin build.  
It wraps the generator and provides a clean DSL for generating java code from your SysML based mission models 
as part of your gradle build pipeline.

---


## Installation  

### Add the plugin to your `build.gradle.kts` (Kotlin DSL)  

```kotlin
plugins {
    // Apply the plugin from Maven Central
    id("fr.inria.mbdo.mission.sysml-mission-generator-gradle-plugin") version "1.2.0-SNAPSHOT"
}
```

> **Tip:** If you prefer the Groovy DSL, use the same coordinates in `build.gradle`:



### Declare the repository 

In order to find the gradle plugin and resolve it, it is currently required to add the repository and resolutionStrategy in the `pluginManagement` section.

example `settings.gradle.kts` 
```kotlin
pluginManagement {
    repositories {
        mavenLocal() // For locally built and installed plugins

        maven { // For released plugins
            url = uri("https://maven.pkg.github.com/edt-edu/cps-fischertechnik")

            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull

                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }

        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id ==
                "fr.inria.mbdo.mission.sysml-mission-generator-gradle-plugin"
            ) {
                useModule(
                    "fr.inria.mbdo.mission:sysml-mission-generator-gradle-plugin:${requested.version}"
                )
            }
        }
    }
}
```
---

## Usage  



### Basic task

```bash
./gradlew generateSysml
```

The task generates Java sources under `build/generated/sources/sysml/main/java` (by default).
The output directory is added to the `main` source set, so `compileJava` (and any other consumer of the sources) runs `generateSysml` first automatically.

### Incremental build: when is the generator executed?

`generateSysml` declares its inputs and outputs, so Gradle skips it (`UP-TO-DATE`) unless one of these changed:

- the **content** of one of the `inputFiles`, or their order,
- `basePackageName` or `cleanOutputDirectory`,
- the plugin implementation (e.g. a new version of the generator),
- the generated files under `<outputDirectory>/<basePackageName as path>` (modified or deleted).

`inputFiles` must list **all** the SysML files of the project: files reached only through an `import` are neither loaded nor tracked.

Only the `basePackageName` subtree of the output directory is tracked, so hand-written sources located elsewhere in a shared `outputDirectory` do not trigger a new generation.

To force a generation:

```bash
./gradlew generateSysml --rerun
```

(`./gradlew clean` also works when `outputDirectory` is inside the build directory, which is the default.)

### Removing stale generated files

By default, files are only (over)written: a file generated from a SysML element that was later renamed or removed stays in the output directory and is still compiled.
Set `cleanOutputDirectory = true` to delete the `basePackageName` subtree of the output directory before each generation.

> **Warning:** every file in this package subtree is deleted, including hand-written ones. When `outputDirectory` is outside the build directory, the task logs a warning each time it cleans it. Keep a clear package separation between generated and hand-written code.

> **Limitation:** when `basePackageName` changes, the files of the *previous* package are not deleted. Remove the old package folder manually (or run `./gradlew clean` when the output is inside the build directory).

### Example `build.gradle.kts` with custom configuration

```kotlin
sysmlGenerator {
    inputFiles = files(	"src/main/sysml/common/common_def.sysml",
        "src/main/sysml/CB/cb_def.sysml",
        "src/main/sysml/VGR/vgr_def.sysml",
        "src/main/sysml/CB/cb_missions_def.sysml")
    basePackageName = "com.example.generated"
    outputDirectory = layout.buildDirectory.dir("generated-src/java")
}
```


---

## Configuration Options  

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `inputFiles`      | `ListProperty<File>` |  | Ordered list of sysml files |
| `basePackageName` | `String` | `fr.inria.generated` | base java package name for generated files |
| `outputDirectory` | `DirectoryProperty` | `build/generated/sources/sysml/main/java/` | Where generated artifacts are placed. |
| `cleanOutputDirectory` | `Boolean` | `false` | Delete the `basePackageName` subtree of `outputDirectory` before each generation (see above). |

All properties are exposed under the `sysmlGenerator` extension block.

---


## Development & Contributing  

### Build and install the plugin locally  

from the root of the `sysml-mission-generator`

```bash
mvn install
```

(optionnaly add `-DskipTests` to avoid running the tests)

This will install the plugin in you `.m2` local maven repository.

However, most changes usually occurs in the `sysml-mission-generator` module, sot the recommanded command is to run `mvn clean install` in the parent folder.


> **Tip** The project used as fixture for the integration test can be used directly as an example. it is located in `sysml-mission-generator/sysml-mission-generator-gradle-plugin/src/test/resources/mission-gradle-project-example`

* Check used jar and dependencies: 
from a sample gradle project that use the generator:
```
./gradlew buildEnvironment
```

---
