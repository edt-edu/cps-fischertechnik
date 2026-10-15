# sysml‑mission‑generator‑maven‑plugin

## Overview
`sysml‑mission‑generator‑maven‑plugin` is a Maven plugin that generates Java source code from one or more SysML model files during the **generate‑sources** phase of a Maven build.  
The generated code depends on the **`sysml‑mission‑runtime`** library, which provides the runtime API used by the produced classes.

## Features
| Feature | Description |
|---------|-------------|
| **Multi‑file input** | Accepts any number of SysML files (`.sysml`). |
| **Package control** | Lets you set the base Java package for the generated sources. |
| **Custom output directory** | Generates sources into `target/generated-sources/sysml` (configurable). |
| **Integration‑ready** | Works with standard Maven lifecycles; optionally add the `build-helper-maven-plugin` to make the generated sources part of the compile classpath automatically. |

## Prerequisites
- **Java 21 +** (however, the generated code targets the JDK you compile with).  
- **Maven 3.6 +**.  
- The **`sysml‑mission‑runtime`** library must be available as a dependency in the consuming project (see the example below).

## Adding the plugin to a Maven project
Add the plugin declaration inside the `<plugins>` section of your project's `pom.xml`. A minimal working configuration looks like this:

```xml
<build>
    <plugins>
        <!-- SysML Mission Generator Maven Plugin -->
        <plugin>
            <groupId>fr.inria.mbdo.mission</groupId>
            <artifactId>sysml-mission-generator-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>

            <!-- bind the goal to the generate-sources phase -->
            <executions>
                <execution>
                    <id>generate-sysml</id>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>

            <configuration>
                <!-- List of SysML files to process -->
                <inputFiles>
                    <file>../missions-design-models/common/common_def.sysml</file>
                    <file>../missions-design-models/zones/zones_def.sysml</file>
                    <file>../missions-design-models/CB/cb_def.sysml</file>
                    <file>../missions-design-models/VGR/vgr_def.sysml</file>
                    <file>../missions-design-models/CB/cb_missions_def.sysml</file>
                </inputFiles>

                <!-- Base package for generated Java classes -->
                <basePackageName>fr.inria.mbdo.factory.generated</basePackageName>

                <!-- Optional: override the default output directory -->
                <!-- <outputDirectory>${project.build.directory}/generated-sources/sysml</outputDirectory> -->
            </configuration>
        </plugin>

        <!-- Optional: add the generated sources to the compile classpath, not mandatory but may help some IDE such as eclipse -->
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>build-helper-maven-plugin</artifactId>
            <version>3.2.0</version>
            <executions>
                <execution>
                    <id>add-generated-sources</id>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>add-source</goal>
                    </goals>
                    <configuration>
                        <sources>
                            <source>${project.build.directory}/generated-sources/sysml</source>
                        </sources>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Required project dependencies
The generated code relies on the runtime library. Add it as a regular Maven dependency:

```xml
<dependencies>
    <dependency>
        <groupId>fr.inria.mbdo.mission</groupId>
        <artifactId>sysml-mission-runtime</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Configuration parameters
| Parameter | Required? | Description |
|-----------|-----------|-------------|
| `inputFiles` | **Yes** | `<inputFiles>` is a list of `<file>` entries, each pointing to a SysML model file to be processed. Paths are resolved relative to the module’s directory. |
| `basePackageName` | **Yes** | The base Java package for all generated classes (e.g. `fr.inria.mbdo.factory.generated`). |
| `outputDirectory` | No (default: `${project.build.directory}/generated-sources/sysml`) | Where the plugin writes the generated source tree. |
| `skip` | No (default: `false`) | Set to `true` to disable generation for a particular build. |
| `verbose` | No (default: `false`) | If `true`, the plugin logs each processed file and generated class. |

All parameters are set inside the `<configuration>` block of the plugin declaration.

## Running the generation
The plugin is bound to the **`generate-sources`** phase by default, so a normal Maven build will trigger the code generation automatically:

```bash
mvn clean compile   # generates sources then compiles them
```

If you want to run the generator without a full compile, you can invoke the specific goal:

```bash
mvn fr.inria.mbdo.mission:sysml-mission-generator-maven-plugin:generate-sources
```

After execution, the generated sources are located under `target/generated-sources/sysml`. If you added the `build-helper-maven-plugin` as shown above, these sources are automatically added to the project's compile classpath.


## Building the plugin itself
If you need to modify or rebuild the plugin:

```bash
cd sysml-mission-generator-maven-plugin
mvn clean install   # installs the plugin into your local Maven repository
```

The resulting artifact will be available as `fr.inria.mbdo.mission:sysml-mission-generator-maven-plugin:1.0.0-SNAPSHOT`.


However, most changes usually occurs in the `sysml-mission-generator` module, sot the recommanded command is to run `mvn clean install` in the parent folder.

---

## License
This project is licensed under the **Apache License, Version 2.0**. See the `LICENSE` file for details.

---
