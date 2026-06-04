# Scripts

## generate-from-sysml.sh

Builds the project and generates Java mission code from SysML v2 model files in a single command.

### Prerequisites

- Java 21+
- Maven (accessible as `mvn`)

### Usage

```bash
./scripts/generate-from-sysml.sh <output-dir> <package-name> <sysml-inputs...>
```

| Argument | Description |
|---|---|
| `output-dir` | Path to the output folder for generated Java sources |
| `package-name` | Base Java package name (e.g. `fr.inria.mbdo.mission.extensions.ren_mission_01`) |
| `sysml-inputs` | One or more: file paths, directories, or glob patterns |

### Input resolution

Each `sysml-input` argument is resolved as follows:

| Input type | Behavior |
|---|---|
| Directory | Searched recursively for all `*.sysml` files |
| File path | Used directly |
| Glob pattern | Expanded (supports `**` for recursive matching) |

You can mix all three in the same invocation.

### Examples

```bash
# All .sysml files inside a directory (recursive)
./scripts/generate-from-sysml.sh ./tmp/generated com.example /path/to/models

# Glob pattern
./scripts/generate-from-sysml.sh ./tmp/generated com.example "/path/to/models/**/*.sysml"

# Explicit files
./scripts/generate-from-sysml.sh ./tmp/generated com.example common.sysml cb_def.sysml

# Mix: a directory + extra files
./scripts/generate-from-sysml.sh ./tmp/generated com.example ./models extra/custom.sysml
```

``` bash
./scripts/generate-from-sysml.sh ./tmp/generated fr.inria.mbdo.mission.extensions.ren_mission_01 \
../missions-design-models/common/common_def.sysml \
../missions-design-models/CB/cb_def.sysml \
../missions-design-models/SL/sl_def.sysml \
../missions-design-models/MPS/mps_def.sysml \
../missions-design-models/VGR/vgr_def.sysml \
../missions-design-models/zones/zones_def.sysml \
../missions-design-models/CB/cb_missions_def.sysml \
../missions-design-models/SL/sl_missions_def.sysml \
../missions-design-models/MPS/mps_missions_def.sysml \
../missions-design-models/VGR/vgr_missions_def.sysml \
../missions-design-models/zones/zones_missions_def.sysml
```

### What it does

1. Resolves all SysML input files from the provided arguments
2. Rebuilds the project (`mvn package -DskipTests`) to produce the CLI fat jar
3. Runs the generator via `java -jar` with the resolved inputs

The fat jar is produced by the `maven-shade-plugin` configured in `pom.xml` and bundles all runtime dependencies (picocli, SySon, JavaPoet, etc.) into a single executable jar.

### Troubleshooting

| Problem | Fix |
|---|---|
| `No files matched` | Check your glob pattern is quoted to prevent premature shell expansion |
| `Build succeeded but CLI jar not found` | Run `mvn package -DskipTests` manually to see build errors |
| `NoClassDefFoundError` | The fat jar is stale; the script rebuilds automatically, but verify Maven completes without errors |
