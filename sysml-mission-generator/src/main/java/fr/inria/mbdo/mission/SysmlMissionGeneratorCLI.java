package fr.inria.mbdo.mission;

import java.io.IOException;

/**
 * Hello world!
 */
public class SysmlMissionGeneratorCLI {
    public static void main(String[] args) {
    	if (args.length < 2) {
            System.err.println("""
                    Usage: SysmlMissionGenerator <sysmlFilePath> <targetFolderPath>
                        <sysmlFilePath> : Path to the SysML file
                        <targetFilePath> : Path to output folder
                    """);
            return;
        }

        String sysmlFilePath = args[0];
        String targetFolderPath = args[1];
        try {
			new SysmlMissionGenerator().generate(sysmlFilePath, targetFolderPath);
		} catch (IOException e) {
			System.err.println("Error while generating Sysml mission from input file : " + sysmlFilePath);
			e.printStackTrace();
		}
    }
}
