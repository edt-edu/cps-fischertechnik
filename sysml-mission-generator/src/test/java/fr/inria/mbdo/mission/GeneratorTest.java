package fr.inria.mbdo.mission;

import com.palantir.javapoet.JavaFile;
import fr.inria.mbdo.mission.generators.GlobalGenerator;
import fr.inria.mbdo.mission.generators.TransformationContext;
import fr.inria.mbdo.mission.importer.SysmlImporter;
import fr.inria.mbdo.mission.utils.ImportUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratorTest {
	private static final Logger logger = LoggerFactory.getLogger(GeneratorTest.class);
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
	void generateMachineMissions()  throws Exception {
		SysmlImporter importer = new SysmlImporter();

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		int nbLibResources = resourceSet.getResources().size();


		// files from ../missions-design-models relatively to the folder containing pom.xml
		Path projectPath = Paths.get(System.getProperty("user.dir"))
				.resolve("../missions-design-models")
				.normalize();

		logger.debug("projectPath: {}", projectPath);
		List<String> fileNames = List.of(
				"common/messages_def.sysml",
				"common/zones_def.sysml",
				"CB/cb_def.sysml",
				"MPS/mps_def.sysml",
				"SL/sl_def.sysml",
				"VGR/vgr_def.sysml",
				//"CB/cb_missions_def.sysml",
				//"MPS/mps_missions_def.sysml",
				"SL/sl_missions_def.sysml"//,
				//"VGR/vgr_missions_def.sysml"
		);

		List<File> files = new ArrayList<File>();
		for(String fileName : fileNames) {

			File sysmlFile = projectPath.resolve(fileName).toFile();
			assertTrue(sysmlFile.exists());
			files.add(ImportUtils.applyFileTransformations(sysmlFile));
		}

		List<Resource> resources = importer.importSysmlTexts(files, resourceSet);
		assertTrue(resources.size() == fileNames.size());
		assertTrue(resourceSet.getResources().size() == nbLibResources+fileNames.size());

        TransformationContext context = new TransformationContext("fr.inria.factoryscada.sysmlbaseddomain");

		for (Resource resource : resources) {
			GlobalGenerator generator = new GlobalGenerator(context);

			List<JavaFile> generatedFiles = generator.generate(resource.getContents().getFirst());

			generatedFiles.forEach((f ->
				logger.info("Generated class for {}:\n{}", f.packageName(), f))
			);
		}

	}
}
