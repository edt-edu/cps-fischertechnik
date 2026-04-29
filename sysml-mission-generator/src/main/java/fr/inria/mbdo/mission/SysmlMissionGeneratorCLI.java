package fr.inria.mbdo.mission;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "SysmlMissionGenerator", 
	description = "Generates missions java code from one or more SysML files.")
public class SysmlMissionGeneratorCLI implements Callable<Integer> {
	@Option(names = { "-i", "--input" }, 
			required = true, 
			description = "Path(s) to SysML file(s)", 
			arity = "1..*")
	private List<String> sysmlFilePaths;

	@Option(names = { "-p", "--package" }, 
			required = true, 
			description = "base âckage name")
	private String basePackageName;
	
	@Option(names = { "-o", "--output" }, 
			required = true, 
			description = "Path to output folder")
	private String targetFolderPath;

    @CommandLine.Option(
        names = {"-h", "--help"},
        usageHelp = true,
        description = "Display this help message"
    )
    boolean helpRequested;
    
	public static void main(String[] args) {
		int exitCode = new CommandLine(new SysmlMissionGeneratorCLI()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() {
		SysmlMissionGenerator generator = new SysmlMissionGenerator();

		List<File> inputFiles = sysmlFilePaths.stream()
			    .map(File::new)
			    .toList();
		
		try {
				generator.generate(inputFiles, basePackageName, new File(targetFolderPath));
			} catch (IOException e) {
				System.err.println("Error while generating Sysml mission from input files");
				e.printStackTrace();
				return 1;
			}
		
		return 0;
	}

}
