package fr.inria.mbdo.mission;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysmlMissionGeneratorTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(SysmlMissionGeneratorTest.class);
	@TempDir
	Path tempDir;

	@BeforeAll
	static void checkEnvironment() {
		TestUtils.assertNodeAvailable();
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
		SysmlMissionGenerator generator = new SysmlMissionGenerator();

		// files form /src/test/resources
		File sysmlFile = new File(getClass().getClassLoader().getResource(sysmlFileName).toURI());
		assertTrue(sysmlFile.exists());

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		generator.importSysml(sysmlFile, resourceSet);

		assertTrue(
				resourceSet.getResources().stream().anyMatch(r -> r.getURI().toString().contains(sysmlFileName)),
				() -> "Cannot find resource relative to " + sysmlFileName);

	}

}
