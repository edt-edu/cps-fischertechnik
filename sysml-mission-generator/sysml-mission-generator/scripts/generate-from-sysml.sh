#!/usr/bin/env bash
#
# Generate Java mission code from SysML files.
#
# Usage:
#   ./scripts/generate-from-sysml.sh <output-dir> <package-name> <sysml-inputs...>
#
# Arguments:
#   output-dir      Path to the output folder for generated Java sources
#   package-name    Base Java package name
#   sysml-inputs    One or more: file paths, directories, or glob patterns
#                   Directories are searched recursively for *.sysml files.
#                   Glob patterns (e.g. "models/**/*.sysml") are expanded.
#
# Examples:
#   # Explicit files
#   ./scripts/generate-from-sysml.sh ./out com.example file1.sysml file2.sysml
#
#   # All .sysml in a directory (recursive)
#   ./scripts/generate-from-sysml.sh ./out com.example /path/to/models
#
#   # Glob pattern
#   ./scripts/generate-from-sysml.sh ./out com.example "/path/to/models/**/*.sysml"
#
#   # Mix of everything
#   ./scripts/generate-from-sysml.sh ./out com.example /path/to/models extra.sysml

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# ─── Argument parsing ──────────────────────────────────────────────────────────
if [ $# -lt 3 ]; then
    echo "Usage: $0 <output-dir> <package-name> <sysml-inputs...>"
    echo ""
    echo "  output-dir      Path to the output folder for generated Java sources"
    echo "  package-name    Base Java package name"
    echo "  sysml-inputs    Files, directories (searched recursively), or glob patterns"
    echo ""
    echo "Examples:"
    echo "  $0 ./out com.example /path/to/models"
    echo "  $0 ./out com.example \"models/**/*.sysml\""
    echo "  $0 ./out com.example file1.sysml dir/ \"other/*.sysml\""
    exit 1
fi

OUTPUT_DIR="$1"
PACKAGE_NAME="$2"
shift 2

# ─── Resolve SysML input files ─────────────────────────────────────────────────
INPUT_FILES=()

for input in "$@"; do
    if [ -d "$input" ]; then
        # Directory: find all .sysml files recursively
        while IFS= read -r -d '' f; do
            INPUT_FILES+=("$f")
        done < <(find "$input" -type f -name '*.sysml' -print0 | sort -z)
    elif [ -f "$input" ]; then
        # Exact file
        INPUT_FILES+=("$input")
    else
        # Treat as glob pattern
        shopt -s nullglob globstar
        expanded=($input)
        shopt -u nullglob globstar
        if [ ${#expanded[@]} -eq 0 ]; then
            echo "ERROR: No files matched: $input"
            exit 1
        fi
        for f in "${expanded[@]}"; do
            [ -f "$f" ] && INPUT_FILES+=("$f")
        done
    fi
done

if [ ${#INPUT_FILES[@]} -eq 0 ]; then
    echo "ERROR: No SysML files found from the provided inputs."
    exit 1
fi

echo "Found ${#INPUT_FILES[@]} SysML file(s):"
for f in "${INPUT_FILES[@]}"; do
    echo "  $f"
done
echo ""

# ─── Always rebuild the CLI fat jar ───────────────────────────────────────────
echo "Building CLI jar (mvn package -DskipTests)..."
mvn -f "$PROJECT_DIR/pom.xml" package -DskipTests -q

CLI_JAR="$(find "$PROJECT_DIR/target" -name '*-cli.jar' | head -1)"

if [ -z "$CLI_JAR" ] || [ ! -f "$CLI_JAR" ]; then
    echo "ERROR: Build succeeded but CLI jar not found."
    echo "  Expected: target/sysml-mission-generator-*-cli.jar"
    exit 1
fi

# ─── Run the generator ─────────────────────────────────────────────────────────
mkdir -p "$OUTPUT_DIR"

INPUT_CLI_ARGS=()
for f in "${INPUT_FILES[@]}"; do
    INPUT_CLI_ARGS+=("-i" "$f")
done

echo "Generating Java code..."
echo "  Package: $PACKAGE_NAME"
echo "  Output:  $OUTPUT_DIR"
echo ""

java -jar "$CLI_JAR" \
    "${INPUT_CLI_ARGS[@]}" \
    -p "$PACKAGE_NAME" \
    -o "$OUTPUT_DIR"

echo ""
echo "Done. Generated files in: $OUTPUT_DIR"
