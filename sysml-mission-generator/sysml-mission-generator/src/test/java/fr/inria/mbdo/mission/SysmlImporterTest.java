package fr.inria.mbdo.mission;

import static fr.inria.mbdo.mission.utils.FileUtils.getResourcePath;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import fr.inria.mbdo.mission.utils.ImportUtils;

/**
 * Integration test suite for {@link SysmlImporter}.
 *
 * <p>Purpose
 *   <ul>
 *     <li>Validate that standalone SysML examples (e.g. the Door‑Controller model) can be
 *         imported without errors.</li>
 *     <li>Ensure the importer registers each created {@link Resource} in the EMF
 *         {@link ResourceSet} and that the imported model contains content.</li>
 *     <li>Guard against regressions in the Syson importer:
 *         <ul>
 *           <li>{@code testSysonImportLimitationStillActive1()} and
 *               {@code testSysonImportLimitationStillActive2()} verify that a known
 *               warning (“Unable to resolve name …”) is still emitted. When the upstream
 *               Syson library fixes this limitation the tests will fail, signalling that
 *               the workaround can be removed.</li>
 *         </ul>
 *     </li>
 *     <li>Confirm that all mission‑design models (common, zones, VGR, CB, MPS, SL) can be
 *         imported together and that no errors are reported.</li>
 *     <li>Check that importing the same set of files in an incorrect dependency order
 *         triggers a single expected error, proving that the importer enforces proper
 *         model ordering.</li>
 *   </ul>
 *
 * <p>Infrastructure
 *   <ul>
 *     <li>{@code @BeforeAll} runs {@link TestUtils#assertNodeAvailable()} to guarantee that
 *         the Node runtime required by the EMF infrastructure is present.</li>
 *     <li>{@code @BeforeEach} logs the start of each test method via SLF4J, aiding debugging
 *         when the suite is executed in CI.</li>
 *   </ul>
 *
 * <p>These tests serve as a regression shield for any change to {@link SysmlImporter},
 * the underlying Syson library, or the EMF resource handling logic.
 */
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
	void import_standalone_example_DoorControllerSystem() throws Exception {
		importTest("standalone_examples/DoorControllerSystem/door-controller-system.sysml");
	}
	
	void importTest(String sysmlFileName) throws Exception {
		SysmlImporter generator = new SysmlImporter();

		// files from /src/test/resources
		File sysmlFile = new File(getResourcePath(sysmlFileName).toUri());
		assertTrue(sysmlFile.exists());

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		int nbLibResources = resourceSet.getResources().size();
		Resource res = generator.importSysmlText(sysmlFile, resourceSet);

		assertNotNull(res);
		assertTrue(resourceSet.getResources().size() == nbLibResources+1, () -> "resourceSet.getResources().size()="+resourceSet.getResources().size()+" nbLibResources="+nbLibResources);

		assertTrue(!res.getContents().isEmpty(), () -> "imported resource is empty");
		
		/* assertion disabled until we can remove ImportUtils.applyFileTransformations() workaround 
		 assertTrue(
				resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
				() -> "Cannot find resource relative to " + sysmlFileName);
		 */
	}
	
	
	@Test
	/**
	 * this test verifies if the limitation in syson importer is still active, the test currently pass if the importer reports an error.
	 * If after a version bump of syson, this test fails because it doesn't report error anymore, this means that we can remove the workaround and simplify  our models
	 * @throws Exception
	 */
	void testSysonImportLimitationStillActive1() throws Exception {
		SysmlImporter generator = new SysmlImporter();

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		int nbLibResources = resourceSet.getResources().size();
		
		// files from /src/test/resources
		List<String> fileNames = List.of("TestMemberAccessImport/TestMemberAccessSendTo.sysml");
		List<File> files = new ArrayList<File>();
		for(String fileName : fileNames) {
			File sysmlFile = new File(getResourcePath(fileName).toUri());
			assertTrue(sysmlFile.exists());
			files.add(sysmlFile);
		}

		List<Resource> resources = generator.importSysmlTexts(files, resourceSet);
		assertTrue(resources.size() == fileNames.size());
		assertTrue(resourceSet.getResources().size() == nbLibResources+fileNames.size());
		if(generator.errorMessages.size() == 1) {
			assertTrue(generator.errorMessages.get(0).contains("WARNING - Unable to resolve name 'sender' for reference 'memberElement'"), 
				() -> "this test fails because it doesn't report the 'Unable to resolve name 'sender'' error anymore but another error: "+generator.errorMessages.get(0));
		}
		assertTrue(generator.errorMessages.size() == 1,() -> "this test fails because it doesn't report error anymore, this probably means that we can remove the workaround and simplify our models and remove this test 🙂");
		for(Resource res : resources) {
			assertTrue(!res.getContents().isEmpty(), () -> "imported resource is empty");
		}
		/* assertion disabled until we can remove ImportUtils.applyFileTransformations() workaround 
		for(String sysmlFileName : fileNames) {
			assertTrue(
					resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
					() -> "Cannot find resource relative to " + sysmlFileName);
		}
		*/
	}
	
	@Test
	/**
	 * this test verifies if the limitation in syson importer is still active, the test currently pass if the importer reports an error.
	 * If after a version bump of syson, this test fails because it doesn't report error anymore, this means that we can remove the workaround and simplify  our models
	 * @throws Exception
	 */
	void testSysonImportLimitationStillActive2() throws Exception {
		SysmlImporter generator = new SysmlImporter();

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		int nbLibResources = resourceSet.getResources().size();
		
		// files from /src/test/resources
		List<String> fileNames = List.of("TestMemberAccessImport/TestMemberAccessAcceptIf.sysml");
		List<File> files = new ArrayList<File>();
		for(String fileName : fileNames) {
			File sysmlFile = new File(getResourcePath(fileName).toUri());
			assertTrue(sysmlFile.exists());
			files.add(sysmlFile);
		}

		List<Resource> resources = generator.importSysmlTexts(files, resourceSet);
		assertTrue(resources.size() == fileNames.size());
		assertTrue(resourceSet.getResources().size() == nbLibResources+fileNames.size());

		if(generator.errorMessages.size() == 1) {
			assertTrue(generator.errorMessages.get(0).contains("WARNING - Unable to resolve name 'color' for reference 'memberElement'"), 
				() -> "this test fails because it doesn't report the 'Unable to resolve name 'color'' error anymore but another error: "+generator.errorMessages.get(0));
		}
		assertTrue(generator.errorMessages.size() == 1,() -> "this test fails because it doesn't report error anymore, this probably means that we can remove the workaround and simplify our models and remove this test 🙂");
		for(Resource res : resources) {
			assertTrue(!res.getContents().isEmpty(), () -> "imported resource is empty");
		}
		/* assertion disabled until we can remove ImportUtils.applyFileTransformations() workaround 
		for(String sysmlFileName : fileNames) {
			assertTrue(
					resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
					() -> "Cannot find resource relative to " + sysmlFileName);
		}
		*/
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
		List<String> fileNames = List.of("common/common_def.sysml",
				"zones/zones_def.sysml", 
				"VGR/vgr_def.sysml", 
				"CB/cb_def.sysml",
				"MPS/mps_def.sysml", 
				"SL/sl_def.sysml",
				"VGR/vgr_missions_def.sysml", 
				"SL/sl_missions_def.sysml", 
				"CB/cb_missions_def.sysml", 
				"MPS/mps_missions_def.sysml");
		
		List<File> files = new ArrayList<File>();
		for(String fileName : fileNames) {
			
			File sysmlFile = projectPath.resolve(fileName).toFile();
			assertTrue(sysmlFile.exists(), () -> "Cannot find resource relative to " + fileName + " in " + projectPath.toString());
			files.add(sysmlFile);
		}

		List<Resource> resources = generator.importSysmlTexts(files, resourceSet);
		assertTrue(resources.size() == fileNames.size());
		assertTrue(resourceSet.getResources().size() == nbLibResources+fileNames.size());
		assertTrue(generator.errorMessages.isEmpty());
		for(Resource res : resources) {
			assertTrue(!res.getContents().isEmpty(), () -> "imported resource is empty");
		}
		/* assertion disabled until we can remove ImportUtils.applyFileTransformations() workaround 
		for(String sysmlFileName : fileNames) {
			assertTrue(
					resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
					() -> "Cannot find resource relative to " + sysmlFileName);
		}
		*/
		
	}
	@Test
	/**
	 * the files are loaded without following required dependencies,  generator must report error
	 * @throws Exception
	 */
	void incorrect_import_order_all_files_from_missionsDesignModels() throws Exception {
		SysmlImporter generator = new SysmlImporter();

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		int nbLibResources = resourceSet.getResources().size();
		
		// files from ../missions-design-models relatively to the folder containing pom.xml
		Path projectPath = Paths.get(System.getProperty("user.dir"))
			    .resolve("../missions-design-models")
			    .normalize();
		List<String> fileNames = List.of("common/common_def.sysml",
				"zones/zones_def.sysml", 
				"VGR/vgr_missions_def.sysml", // should be loaded after
				"VGR/vgr_def.sysml", 
				"CB/cb_def.sysml",
				"MPS/mps_def.sysml", 
				"SL/sl_def.sysml", 
				"SL/sl_missions_def.sysml", 
				"CB/cb_missions_def.sysml", 
				"MPS/mps_missions_def.sysml");
		
		List<File> files = new ArrayList<File>();
		for(String fileName : fileNames) {
			
			File sysmlFile = projectPath.resolve(fileName).toFile();
			assertTrue(sysmlFile.exists(), () -> "Cannot find resource relative to " + fileName + " in " + projectPath.toString());
			files.add(sysmlFile);
		}

		List<Resource> resources = generator.importSysmlTexts(files, resourceSet);
		assertTrue(resources.size() == fileNames.size());
		assertTrue(resourceSet.getResources().size() == nbLibResources+fileNames.size());
		assertTrue(generator.errorMessages.size() == 1, () -> "the importer should report an error because the files are loaded without following required dependencies");
		for(Resource res : resources) {
			assertTrue(!res.getContents().isEmpty(), () -> "imported resource is empty");
		}
		/* assertion disabled until we can remove ImportUtils.applyFileTransformations() workaround 
		for(String sysmlFileName : fileNames) {
			assertTrue(
					resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
					() -> "Cannot find resource relative to " + sysmlFileName);
		}
		*/
	}	

}
