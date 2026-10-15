package fr.inria.mbdo.mission.generator;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import fr.inria.mbdo.mission.SysmlMissionGenerator;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.util.List;

/**
 * Gradle plugin to generate Java code from a set of SysML model files.
 *
 * The plugin registers an extension {@code sysmlGenerator} that allows the user to configure the
 * input files, the base package name for generated sources, and the output directory.
 * It also creates a task {@code generateSysml} that performs the generation using the core
 * {@link SysmlMissionGenerator} class.
 */
public class SysmlMissionGeneratorGradlePlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "sysmlGenerator";
    private static final String TASK_NAME = "generateSysml";

    @Override
    public void apply(Project project) {
        // Create the extension that holds configuration values.
        SysmlGeneratorExtension extension = project.getExtensions().create(EXTENSION_NAME, SysmlGeneratorExtension.class, project);

        // Register the generation task.
        TaskProvider<GenerateSysmlTask> taskProvider = project.getTasks().register(TASK_NAME, GenerateSysmlTask.class, task -> {
            task.setGroup("build");
            task.setDescription("Generate Java code from SysML models.");
            task.setExtension(extension);
        });

        // Ensure that the generated sources are part of the main Java source set.
        project.afterEvaluate(p -> {
            SourceSetContainer sourceSets = (SourceSetContainer) p.getProperties().get("sourceSets");
            if (sourceSets != null) {
                SourceSet main = sourceSets.getByName("main");
                main.getJava().srcDir(extension.outputDirectory);
                // Make the generate task a dependency of compileJava so that sources are generated first.
                p.getTasks().named("compileJava").configure(compile -> compile.dependsOn(taskProvider));
            }
        });
    }

    /**
     * Extension exposing configuration options to the build script.
     */
    public static class SysmlGeneratorExtension {
        private final Project project;
        private final ListProperty<File> inputFiles;
        private final Property<String> basePackageName;
        private final DirectoryProperty outputDirectory;

        public SysmlGeneratorExtension(Project project) {
            this.project = project;
            this.inputFiles = project.getObjects().listProperty(File.class);
            this.basePackageName = project.getObjects().property(String.class).convention("fr.inria.generated");
            this.outputDirectory = project.getObjects().directoryProperty()
                    .convention(project.getLayout().getBuildDirectory().dir("generated-sources/sysml"));
        }

        public ListProperty<File> getInputFiles() { return inputFiles; }
        public void setInputFiles(List<File> files) { this.inputFiles.set(files); }

        public Property<String> getBasePackageName() { return basePackageName; }
        public void setBasePackageName(String pkg) { this.basePackageName.set(pkg); }

        public DirectoryProperty getOutputDirectory() { return outputDirectory; }
        public void setOutputDirectory(File dir) { this.outputDirectory.set(dir); }
    }

    /**
     * Task that performs the actual code generation.
     */
    public abstract static class GenerateSysmlTask extends DefaultTask {

        private SysmlGeneratorExtension extension;

        public void setExtension(SysmlGeneratorExtension extension) {
            this.extension = extension;
        }

        @TaskAction
        public void generate() throws Exception {
            if (extension == null) {
                throw new IllegalStateException("SysmlGeneratorExtension not set on task.");
            }
            List<File> inputs = extension.getInputFiles().get();
            String basePkg = extension.getBasePackageName().get();
            File outDir = extension.getOutputDirectory().get().getAsFile();

            // Ensure output directory exists.
            if (!outDir.exists() && !outDir.mkdirs()) {
                throw new RuntimeException("Could not create output directory: " + outDir);
            }
            
            // Log the parameters that will be used for generation.
            getLogger().info("SysML generator parameters: inputFiles=" + inputs + ", basePackage='" + basePkg + "', outputDirectory='" + outDir + "'");
            // Invoke core generator.
            SysmlMissionGenerator generator = new SysmlMissionGenerator();
            generator.generate(inputs, basePkg, outDir);
        }
    }
}
