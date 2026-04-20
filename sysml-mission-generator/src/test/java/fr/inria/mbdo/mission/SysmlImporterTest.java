package fr.inria.mbdo.mission;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

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
		File sysmlFile = new File(getClass().getClassLoader().getResource(sysmlFileName).toURI());
		assertTrue(sysmlFile.exists());

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		generator.importSysmlText(sysmlFile, resourceSet);

		assertTrue(
				resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
				() -> "Cannot find resource relative to " + sysmlFileName);

	}

}
