package fr.inria.mbdo.mission.generator;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

import fr.inria.mbdo.mission.SysmlMissionGenerator;

/**
 * Gradle plugin to generate Java code from a set of SysML model files.
 *
 * <p>The plugin registers an extension {@code sysmlGenerator} that allows the user to configure the
 * input files, the base package name for generated sources, and the output directory.
 * It also creates a task {@code generateSysml} that performs the generation using the core
 * {@link SysmlMissionGenerator} class.</p>
 *
 * <p>The task declares its inputs and outputs, so Gradle skips it ({@code UP-TO-DATE}) when neither the
 * SysML files, the configuration, nor the plugin implementation changed. Use
 * {@code ./gradlew generateSysml --rerun} to force a generation.</p>
 */
public class SysmlMissionGeneratorGradlePlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "sysmlGenerator";
    private static final String TASK_NAME = "generateSysml";

    @Override
    public void apply(Project project) {
        // Create the extension that holds configuration values.
        SysmlGeneratorExtension extension = project.getExtensions().create(EXTENSION_NAME, SysmlGeneratorExtension.class, project);

        File projectDir = project.getProjectDir();

        // Register the generation task, wiring its properties lazily from the extension.
        TaskProvider<GenerateSysmlTask> taskProvider = project.getTasks().register(TASK_NAME, GenerateSysmlTask.class, task -> {
            task.setGroup("build");
            task.setDescription("Generate Java code from SysML models.");
            task.getInputFiles().set(extension.getInputFiles());
            task.getInputFileCollection().from(extension.getInputFiles());
            // The generator is sensitive to the order of the input files, while file fingerprints are not.
            task.getInputFileOrder().set(extension.getInputFiles().map(files -> files.stream()
                    .map(f -> relativePath(projectDir, f))
                    .toList()));
            task.getBasePackageName().set(extension.getBasePackageName());
            task.getCleanOutputDirectory().set(extension.getCleanOutputDirectory());
            task.getOutputDirectory().set(extension.getOutputDirectory());
            task.getBuildDirectory().set(project.getLayout().getBuildDirectory());
        });

        // Add the generated sources to the main Java source set. Using a provider derived from the task
        // provider lets Gradle infer the dependency from any consumer (compileJava, sourcesJar, IDE sync...).
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            main.getJava().srcDir(taskProvider.map(task -> task.getOutputDirectory().get()));
        });
    }

    private static String relativePath(File base, File file) {
        Path basePath = base.toPath().toAbsolutePath().normalize();
        Path filePath = file.toPath().toAbsolutePath().normalize();
        try {
            return basePath.relativize(filePath).toString().replace(File.separatorChar, '/');
        } catch (IllegalArgumentException e) {
            // Different roots (e.g. another drive on Windows): fall back to the absolute path.
            return filePath.toString();
        }
    }

    /**
     * Extension exposing configuration options to the build script.
     */
    public static class SysmlGeneratorExtension {
        private final ListProperty<File> inputFiles;
        private final Property<String> basePackageName;
        private final DirectoryProperty outputDirectory;
        private final Property<Boolean> cleanOutputDirectory;

        public SysmlGeneratorExtension(Project project) {
            this.inputFiles = project.getObjects().listProperty(File.class);
            this.basePackageName = project.getObjects().property(String.class).convention("fr.inria.generated");
            this.outputDirectory = project.getObjects().directoryProperty()
                    .convention(project.getLayout().getBuildDirectory().dir("generated/sources/sysml/main/java"));
            this.cleanOutputDirectory = project.getObjects().property(Boolean.class).convention(false);
        }

        /**
         * Ordered list of SysML files to process. It must be the complete set of project model files:
         * imported files that are not listed here are neither loaded nor tracked for changes.
         *
         * @return the ordered list of SysML input files
         */
        public ListProperty<File> getInputFiles() { return inputFiles; }
        public void setInputFiles(List<File> files) { this.inputFiles.set(files); }

        /**
         * Base Java package of the generated classes. All generated files are placed below this package.
         *
         * @return the base package name (default {@code fr.inria.generated})
         */
        public Property<String> getBasePackageName() { return basePackageName; }
        public void setBasePackageName(String pkg) { this.basePackageName.set(pkg); }

        /**
         * Root source directory receiving the generated files; it is added to the {@code main} source set.
         *
         * @return the output directory (default {@code build/generated/sources/sysml/main/java})
         */
        public DirectoryProperty getOutputDirectory() { return outputDirectory; }
        public void setOutputDirectory(File dir) { this.outputDirectory.set(dir); }

        /**
         * When {@code true}, the whole {@code basePackageName} subtree of the output directory is deleted before
         * each generation, so that files generated from removed or renamed SysML elements do not remain.
         * <p><b>Warning:</b> any hand-written file located in this package subtree is deleted too. Files of the
         * previous package are not deleted when {@code basePackageName} changes.</p>
         *
         * @return whether to clean the package subtree before generation (default {@code false})
         */
        public Property<Boolean> getCleanOutputDirectory() { return cleanOutputDirectory; }
        public void setCleanOutputDirectory(boolean clean) { this.cleanOutputDirectory.set(clean); }
    }

    /**
     * Task that performs the actual code generation.
     */
    public abstract static class GenerateSysmlTask extends DefaultTask {

        /** Ordered SysML files, as given to the generator. */
        @Internal
        public abstract ListProperty<File> getInputFiles();

        /** Same files, used by Gradle to fingerprint their content. */
        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract ConfigurableFileCollection getInputFileCollection();

        /** Project-relative paths of the input files, in generation order. */
        @Input
        public abstract ListProperty<String> getInputFileOrder();

        @Input
        public abstract Property<String> getBasePackageName();

        @Input
        public abstract Property<Boolean> getCleanOutputDirectory();

        /** Root source directory; not tracked as a whole as it may be shared with hand-written sources. */
        @Internal
        public abstract DirectoryProperty getOutputDirectory();

        @Internal
        public abstract DirectoryProperty getBuildDirectory();

        @Inject
        protected abstract FileSystemOperations getFileSystemOperations();

        /**
         * The tracked output: only the subtree of the base package, where all generated files are written.
         *
         * @return the base package directory below the output directory
         */
        @OutputDirectory
        public Provider<Directory> getPackageOutputDirectory() {
            return getOutputDirectory().dir(getBasePackageName().map(pkg -> pkg.replace('.', '/')));
        }

        @TaskAction
        public void generate() throws Exception {
            List<File> inputs = getInputFiles().get();
            String basePkg = getBasePackageName().get();
            File outDir = getOutputDirectory().get().getAsFile();
            File packageDir = getPackageOutputDirectory().get().getAsFile();

            if (getCleanOutputDirectory().get()) {
                File buildDir = getBuildDirectory().get().getAsFile();
                if (!isUnder(packageDir, buildDir)) {
                    getLogger().warn("sysmlGenerator.cleanOutputDirectory is enabled and outputDirectory is outside the build directory: "
                            + "every file under '" + packageDir + "' will be deleted before generation. "
                            + "Make sure no hand-written code lives in this package.");
                }
                getLogger().info("Deleting previously generated files in {}", packageDir);
                getFileSystemOperations().delete(spec -> spec.delete(packageDir));
            }

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

        private static boolean isUnder(File file, File dir) {
            Path filePath = file.toPath().toAbsolutePath().normalize();
            Path dirPath = dir.toPath().toAbsolutePath().normalize();
            return filePath.startsWith(dirPath);
        }
    }
}
