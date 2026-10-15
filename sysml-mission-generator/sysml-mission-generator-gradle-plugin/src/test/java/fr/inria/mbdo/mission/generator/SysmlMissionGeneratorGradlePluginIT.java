package fr.inria.mbdo.mission.generator;

import static org.junit.jupiter.api.Assertions.*;
import static org.gradle.testkit.runner.TaskOutcome.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SysmlMissionGeneratorGradlePluginIT {

    private static final String GRADLE_VERSION = "8.11.1";

    @TempDir
    Path projectDir;

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
                Path dest = projectDir.resolve(
                    fixture.relativize(source)
                );

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
     //   arguments.add("--no-daemon");

        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withGradleVersion(GRADLE_VERSION)
            .withPluginClasspath(pluginClasspath())
            .withArguments(arguments)
            .forwardOutput()
            .build();
    }

    @Test
    void shouldGenerateAndCompileJavaSources()
            throws IOException {

        copyFixture();

        // 1. Test generation
        BuildResult generation = runGradle("generateSysml");

        assertEquals(
            SUCCESS,
            generation.task(":generateSysml").getOutcome()
        );

        // 2. Check generated sources
        Path generatedDir = projectDir.resolve(
            "build/generated/sources/sysml/main/java"
        );

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

        // 3. Compile the generated sources
        BuildResult compilation = runGradle("compileJava");

        assertEquals(
            SUCCESS,
            compilation.task(":compileJava").getOutcome()
        );
    }
}