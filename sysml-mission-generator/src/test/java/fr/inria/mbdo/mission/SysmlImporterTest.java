package fr.inria.mbdo.mission;

import static fr.inria.mbdo.mission.utils.FileUtils.getResourcePath;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.mbdo.mission.importer.SysmlImporter;

public class SysmlImporterTest {
	private static final Logger logger = LoggerFactory.getLogger(SysmlImporterTest.class);

	@BeforeAll
	static void checkEnvironment() {
		TestUtils.assertNodeAvailable();
	}

	@BeforeEach
    void logTestStart(TestInfo testInfo) {
        logger.info("=== Running test: {} ===", testInfo.getDisplayName());
    }
	
	@Test
	void import_CB_from_CBVGRMission() throws Exception {
		importTest("CBVGRMission/CB.sysml");
	}

	@Test
	void import_VGR_from_CBVGRMission() throws Exception {
		importTest("CBVGRMission/VGR.sysml");
	}
	
	void importTest(String sysmlFileName) throws Exception {
		SysmlImporter generator = new SysmlImporter();

		// files from /src/test/resources
		File sysmlFile = new File(getResourcePath(sysmlFileName).toUri());
		assertTrue(sysmlFile.exists());

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		generator.importSysmlText(sysmlFile, resourceSet);

		assertTrue(
				resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
				() -> "Cannot find resource relative to " + sysmlFileName);

	}
	
	
	@Test
	void import_all_files_from_MultiFileMission() throws Exception {
		SysmlImporter generator = new SysmlImporter();

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		int nbLibResources = resourceSet.getResources().size();
		
		// files from /src/test/resources
		List<String> fileNames = List.of("MultiFileMission/ConveyorBeltCommands.sysml", "MultiFileMission/ConveyorBelt.sysml");
		List<File> files = new ArrayList<File>();
		for(String fileName : fileNames) {
			File sysmlFile = new File(getClass().getClassLoader().getResource(fileName).toURI());
			assertTrue(sysmlFile.exists());
			files.add(sysmlFile);
		}

		List<Resource> resources = generator.importSysmlTexts(files, resourceSet);
		assertTrue(resources.size() == 2);
		assertTrue(resourceSet.getResources().size() == nbLibResources+2);
		assertTrue(generator.errorMessages.isEmpty());
		for(String sysmlFileName : fileNames) {
			assertTrue(
					resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
					() -> "Cannot find resource relative to " + sysmlFileName);
		}
		
	}
	
	@Test
	/**
	 * the files are loaded without following required dependencies,  generator must report error
	 * @throws Exception
	 */
	void incorrect_import_order_all_files_from_MultiFileMission() throws Exception {
		SysmlImporter generator = new SysmlImporter();

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		int nbLibResources = resourceSet.getResources().size();
		
		// files from /src/test/resources
		List<String> fileNames = List.of("MultiFileMission/ConveyorBelt.sysml", "MultiFileMission/ConveyorBeltCommands.sysml");
		List<File> files = new ArrayList<File>();
		for(String fileName : fileNames) {
			File sysmlFile = new File(getResourcePath(fileName).toUri());
			assertTrue(sysmlFile.exists());
			files.add(sysmlFile);
		}

		List<Resource> resources = generator.importSysmlTexts(files, resourceSet);
		assertTrue(resources.size() == 2);
		assertTrue(resourceSet.getResources().size() == nbLibResources+2);
		assertTrue(generator.errorMessages.size() == 1);
		for(String sysmlFileName : fileNames) {
			assertTrue(
					resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
					() -> "Cannot find resource relative to " + sysmlFileName);
		}
	}
	
	@Test
	void import_all_files_from_missionsDesignModels() throws Exception {
		SysmlImporter generator = new SysmlImporter();

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		int nbLibResources = resourceSet.getResources().size();
		

		// files from ../missions-design-models relatively to the folder containing pom.xml
		Path projectPath = Paths.get(System.getProperty("user.dir"))
			    .resolve("../missions-design-models")
			    .normalize();
		List<String> fileNames = List.of("common/messages_def.sysml"/*, "common/zones_def.sysml"*/);
		
		List<File> files = new ArrayList<File>();
		for(String fileName : fileNames) {
			
			File sysmlFile = projectPath.resolve(fileName).toFile();
			assertTrue(sysmlFile.exists());
			files.add(sysmlFile);
		}

		List<Resource> resources = generator.importSysmlTexts(files, resourceSet);
		assertTrue(resources.size() == fileNames.size());
		assertTrue(resourceSet.getResources().size() == nbLibResources+fileNames.size());
		assertTrue(generator.errorMessages.isEmpty());
		for(String sysmlFileName : fileNames) {
			assertTrue(
					resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
					() -> "Cannot find resource relative to " + sysmlFileName);
		}
		
	}
	

}
