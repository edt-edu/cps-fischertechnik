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
    id("edu.edt.sysml.generator") version "1.0.0-SNAPSHOT"
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
./gradlew generateMission
```

The task generates a SysML model under `build/generated-sysml`.  
It runs **after** `processResources` and **before** `compileJava` by default, so generated sources are compiled automatically.

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
| `basePackageName` | `String` |  | base java package name for generated files |
| `outputDirectory` | `DirectoryProperty` | `build/generated/sources/sysml/main/java/` | Where generated artifacts are placed. |

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


* Check used jar and dependencies: 
from a sample gradle project that use the generator:
```
./gradlew buildEnvironment
```

---
