package fr.inria.mbdo.mission;

import static fr.inria.mbdo.mission.TestUtils.getFilesFromSrcResources;
import static fr.inria.mbdo.mission.utils.FileUtils.getResourcePath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.syson.sysml.Classifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.palantir.javapoet.JavaFile;

import fr.inria.mbdo.mission.generators.MachineInterfaceGenerator;
import fr.inria.mbdo.mission.importer.SysmlImporter;

public class MachineInterfaceGeneratorTest {
	private static final Logger logger = LoggerFactory.getLogger(MachineInterfaceGeneratorTest.class);
	@TempDir
	Path tempDir;

	@BeforeAll
	static void checkEnvironment() {
		TestUtils.assertNodeAvailable();
	}

	@BeforeEach
	void logTestStart(TestInfo testInfo) {
		logger.info("=== Running test: {} ===", testInfo.getDisplayName());
	}

	@Test
	void generateMachineInterfaces_for_MultiFileMission() throws Exception {
		List<String> fileNames = List.of("MultiFileMission/ConveyorBeltCommands.sysml",
				"MultiFileMission/ConveyorBelt.sysml");
		// files from /src/test/resources
		List<File> files = getFilesFromSrcResources(fileNames);
		generateMachineInterfacesForFiles(files,
				"golden/MachineInterfaceGeneratorTest/generateMachineInterfaces_for_MultiFileMission");

	}

	/**
	 * 
	 * @param inputfiles
	 * @param goldenFolderName
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	void generateMachineInterfacesForFiles(List<File> inputFiles, String goldenFolderName)
			throws URISyntaxException, FileNotFoundException, IOException {

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		SysmlImporter importer = new SysmlImporter();
		List<Resource> resources = importer.importSysmlTexts(inputFiles, resourceSet);

		MachineInterfaceGenerator interfaceGenerator = new MachineInterfaceGenerator(
				"fr.inria.factoryscada.sysmlbaseddomain");

		for (Resource res : resources) {
			interfaceGenerator.generate(res.getContents().getFirst()); // TODO deal with multiple root

			for (Entry<Classifier, JavaFile> entry : interfaceGenerator.getContext().classifierToJavaFile.entrySet()) {
				String generated = entry.getValue().toString();
				logger.debug("Generated class for {} {}:\n{}", entry.getKey().eClass().getName(), entry.getKey().effectiveName(), generated);
				Path expectedPath = getResourcePath(goldenFolderName + "/" + entry.getValue().typeSpec().name() + ".java");
				String expected = Files.readString(expectedPath);
				boolean update = Boolean.getBoolean("update.golden");
				if (update) {
					logger.warn("Golden content check bypassed");
					logger.warn("Updating golden file {}", expectedPath);
					Path sourceGoldenPath = Paths.get(System.getProperty("user.dir"))
						    .resolve("src/test/resources")
						    .resolve(goldenFolderName+ "/" + entry.getValue().typeSpec().name() + ".java");
					Files.writeString(sourceGoldenPath, generated);
				} else {
					Assertions.assertEquals(expected, generated,
							() -> "Diff:\nEXPECTED:\n" + expected + "\n\nACTUAL:\n" + generated);
				}
			}
		}
	}
}
