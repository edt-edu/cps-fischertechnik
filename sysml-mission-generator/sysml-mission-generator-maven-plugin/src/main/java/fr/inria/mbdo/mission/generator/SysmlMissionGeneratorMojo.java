package fr.inria.mbdo.mission.generator;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import fr.inria.mbdo.mission.SysmlMissionGenerator;

/**
 * Maven Mojo to trigger SysML code generation during the build lifecycle.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class SysmlMissionGeneratorMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * List of SysML input files to process.
     * <p>
	 * <b>Important:</b> The generator depends on a component that requires models 
	 * to be read in a specific order. Ensure the files are listed accordingly.
	 * </p>
	 * <p>
	 * Recommended location: Place files in <b>src/main/sysml/</b>.
	 * </p>
     */
    @Parameter(required = true)
    private List<File> inputFiles;

    /**
     * Base package name for the generated Java classes.
     */
    @Parameter(defaultValue = "fr.inria.generated", required = true)
    private String basePackageName;

    /**
     * Output directory for generated source files.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/sysml", required = true)
    private File outputDirectory;

    public SysmlMissionGeneratorMojo() {
    	getLog().error("SysmlMissionGeneratorMojo");
	}
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting SysML code generation...");

        if (inputFiles == null || inputFiles.isEmpty()) {
            throw new MojoFailureException("No SysML input files configured in <inputFiles>.");
        }

        // Ensure output directory exists
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new MojoExecutionException("Could not create output directory: " + outputDirectory);
        }

        try {
            // Instantiate core generator
            SysmlMissionGenerator generator = new SysmlMissionGenerator();
            
            getLog().info("Processing " + inputFiles.size() + " files...");
            
            // Logic call (assuming you update your core class to accept these params)
            generator.generate(inputFiles, basePackageName, outputDirectory);

            getLog().info("Generation successful. Files written to: " + outputDirectory.getAbsolutePath());

            // Notify Maven that this directory contains source code to be compiled
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
            getLog().debug("Added " + outputDirectory.getAbsolutePath() + " to Maven compile source roots.");

        } catch (Exception e) {
            getLog().error("Generation failed", e);
            throw new MojoExecutionException("Error during SysML code generation", e);
        }
    }

}
