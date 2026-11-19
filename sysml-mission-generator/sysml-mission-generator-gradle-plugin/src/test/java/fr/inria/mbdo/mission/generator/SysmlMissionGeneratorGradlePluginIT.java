package fr.inria.mbdo.mission.generator;

import static org.junit.jupiter.api.Assertions.*;
import static org.gradle.testkit.runner.TaskOutcome.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SysmlMissionGeneratorGradlePluginIT {

    private static final String GRADLE_VERSION = "8.11.1";
    private static final String TASK = ":generateSysml";
    private static final String DEFAULT_OUTPUT = "build/generated/sources/sysml/main/java";
    private static final String PACKAGE_PATH = "com/example/generated";
    /** Directories of the fixture that may exist locally but must not be copied. */
    private static final Set<String> IGNORED_FIXTURE_DIRS = Set.of("build", ".gradle", "generated-src");

    @TempDir
    Path projectDir;

    @BeforeEach
    void setUp() throws IOException {
        copyFixture();
    }

    private List<File> pluginClasspath() throws IOException {
        List<File> classpath = new ArrayList<>();

        // Maven-compiled plugin classes and plugin descriptor
        classpath.add(Path.of("target/classes").toFile());

        // Runtime dependencies prepared by Maven
        try (Stream<Path> files = Files.list(
                Path.of("target/plugin-test-libs"))) {

            files.filter(p -> p.toString().endsWith(".jar"))
                 .sorted()
                 .map(Path::toFile)
                 .forEach(classpath::add);
        }

        return classpath;
    }

    private void copyFixture() throws IOException {
        Path fixture = Path.of(
            "src/test/resources/mission-gradle-project-example"
        );

        try (Stream<Path> paths = Files.walk(fixture)) {
            for (Path source : paths.toList()) {
                Path relative = fixture.relativize(source);
                if (relative.getNameCount() > 0
                        && IGNORED_FIXTURE_DIRS.contains(relative.getName(0).toString())) {
                    continue;
                }
                Path dest = projectDir.resolve(relative);

                if (Files.isDirectory(source)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(source, dest);
                }
            }
        }
    }

    private BuildResult runGradle(String... tasks)
            throws IOException {

        List<String> arguments = new ArrayList<>(List.of(tasks));
        arguments.add("--stacktrace");

        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withGradleVersion(GRADLE_VERSION)
            .withPluginClasspath(pluginClasspath())
            .withArguments(arguments)
            .forwardOutput()
            .build();
    }

    /** Replaces text in the fixture build script; fails if the text is not found. */
    private void editBuildScript(String target, String replacement) throws IOException {
        Path buildFile = projectDir.resolve("build.gradle");
        String content = Files.readString(buildFile);
        assertTrue(content.contains(target), "build.gradle does not contain: " + target);
        Files.writeString(buildFile, content.replace(target, replacement));
    }

    /** Adds a line to the generator configuration block of the fixture build script. */
    private void addGeneratorOption(String line) throws IOException {
        editBuildScript("sysmlGenerator {", "sysmlGenerator {\n    " + line);
    }

    private Path writeJavaFile(String relativeDir, String className, String pkg) throws IOException {
        Path file = projectDir.resolve(relativeDir).resolve(className + ".java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "package " + pkg + ";\npublic class " + className + " {}\n");
        return file;
    }

    @Test
    void shouldGenerateAndCompileJavaSources()
            throws IOException {

        // 1. Compile directly: generation must be triggered through the source set wiring
        BuildResult compilation = runGradle("compileJava");

        assertEquals(SUCCESS, compilation.task(TASK).getOutcome());
        assertEquals(SUCCESS, compilation.task(":compileJava").getOutcome());

        // 2. Check generated sources
        Path generatedDir = projectDir.resolve(DEFAULT_OUTPUT).resolve(PACKAGE_PATH);

        assertTrue(
            Files.isDirectory(generatedDir),
            "Generated source directory does not exist"
        );

        try (Stream<Path> files = Files.walk(generatedDir)) {
            assertTrue(
                files.anyMatch(p ->
                    p.toString().endsWith(".java")
                ),
                "No Java sources were generated"
            );
        }
    }

    @Test
    void shouldBeUpToDateWhenNothingChanged() throws IOException {
        assertEquals(SUCCESS, runGradle("generateSysml").task(TASK).getOutcome());
        assertEquals(UP_TO_DATE, runGradle("generateSysml").task(TASK).getOutcome());
    }

    @Test
    void shouldRegenerateWhenSysmlContentChanges() throws IOException {
        assertEquals(SUCCESS, runGradle("generateSysml").task(TASK).getOutcome());

        Path sysml = projectDir.resolve("src/main/sysml/CB/cb_def.sysml");
        Files.writeString(sysml, "\n// touched by test\n", StandardOpenOption.APPEND);

        assertEquals(SUCCESS, runGradle("generateSysml").task(TASK).getOutcome());
    }

    @Test
    void shouldRegenerateWhenBasePackageChanges() throws IOException {
        assertEquals(SUCCESS, runGradle("generateSysml").task(TASK).getOutcome());

        editBuildScript("basePackageName = 'com.example.generated'", "basePackageName = 'com.example.other'");

        assertEquals(SUCCESS, runGradle("generateSysml").task(TASK).getOutcome());
        assertTrue(Files.isDirectory(projectDir.resolve(DEFAULT_OUTPUT).resolve("com/example/other")));
    }

    @Test
    void shouldRegenerateWithRerun() throws IOException {
        assertEquals(SUCCESS, runGradle("generateSysml").task(TASK).getOutcome());
        assertEquals(SUCCESS, runGradle("generateSysml", "--rerun").task(TASK).getOutcome());
    }

    @Test
    void shouldNotTrackHandWrittenFilesOutsideBasePackage() throws IOException {
        assertEquals(SUCCESS, runGradle("generateSysml").task(TASK).getOutcome());

        // A file in the output root, but outside the base package, must not invalidate the task
        writeJavaFile(DEFAULT_OUTPUT + "/com/example/handwritten", "Manual", "com.example.handwritten");

        assertEquals(UP_TO_DATE, runGradle("generateSysml").task(TASK).getOutcome());
    }

    @Test
    void shouldKeepStaleFilesByDefault() throws IOException {
        assertEquals(SUCCESS, runGradle("generateSysml").task(TASK).getOutcome());
        Path stale = writeJavaFile(DEFAULT_OUTPUT + "/" + PACKAGE_PATH, "Stale", "com.example.generated");

        assertEquals(SUCCESS, runGradle("generateSysml", "--rerun").task(TASK).getOutcome());

        assertTrue(Files.exists(stale), "Stale file should be kept when cleanOutputDirectory is disabled");
    }

    @Test
    void shouldDeleteStaleFilesOnlyInBasePackageWhenCleanEnabled() throws IOException {
        addGeneratorOption("cleanOutputDirectory = true");
        assertEquals(SUCCESS, runGradle("generateSysml").task(TASK).getOutcome());

        Path stale = writeJavaFile(DEFAULT_OUTPUT + "/" + PACKAGE_PATH, "Stale", "com.example.generated");
        Path outside = writeJavaFile(DEFAULT_OUTPUT + "/com/example/handwritten", "Manual", "com.example.handwritten");

        BuildResult result = runGradle("generateSysml", "--rerun");

        assertEquals(SUCCESS, result.task(TASK).getOutcome());
        assertFalse(Files.exists(stale), "Stale file in the base package should be deleted");
        assertTrue(Files.exists(outside), "File outside the base package must be kept");
        assertFalse(result.getOutput().contains("cleanOutputDirectory is enabled"),
                "No warning expected when the output is inside the build directory");
    }

    @Test
    void shouldWarnWhenCleaningOutsideBuildDirectory() throws IOException {
        addGeneratorOption("cleanOutputDirectory = true");
        addGeneratorOption("outputDirectory = file('generated-src/sysml')");

        BuildResult result = runGradle("compileJava");

        assertEquals(SUCCESS, result.task(TASK).getOutcome());
        assertTrue(result.getOutput().contains("cleanOutputDirectory is enabled"),
                "A warning is expected when cleaning outside the build directory");
        assertTrue(Files.isDirectory(projectDir.resolve("generated-src/sysml").resolve(PACKAGE_PATH)));
    }

    @Test
    void shouldSupportConfigurationCache() throws IOException {
        BuildResult first = runGradle("compileJava", "--configuration-cache");
        assertEquals(SUCCESS, first.task(TASK).getOutcome());

        BuildResult second = runGradle("compileJava", "--configuration-cache");
        assertTrue(second.getOutput().contains("Reusing configuration cache"),
                "Configuration cache should be reused");
        assertEquals(UP_TO_DATE, second.task(TASK).getOutcome());
    }
}
