package fr.inria.mbdo.mission;

import com.palantir.javapoet.JavaFile;
import fr.inria.mbdo.mission.generators.IrRepository;
import fr.inria.mbdo.mission.generators.JavaTransformer;
import fr.inria.mbdo.mission.generators.Linker;
import fr.inria.mbdo.mission.generators.SymbolIndex;
import fr.inria.mbdo.mission.generators.TypeTable;
import fr.inria.mbdo.mission.importer.SysmlImporter;
import fr.inria.mbdo.mission.switchs.IndexerSwitch;
import fr.inria.mbdo.mission.switchs.ToIrSwitch;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MultiPassGenerationTest {
	private static final Logger logger = LoggerFactory.getLogger(MultiPassGenerationTest.class);
	@TempDir
	Path tempDir;

	@BeforeEach
	void logTestStart(TestInfo testInfo) {
		logger.info("=== Running test: {} ===", testInfo.getDisplayName());
	}

	@Test
	void generateMachineMissions() throws Exception {
		SysmlImporter importer = new SysmlImporter();

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		int nbLibResources = resourceSet.getResources().size();

		// files from ../missions-design-models relatively to the folder containing
		// pom.xml
		Path projectPath = Paths.get(System.getProperty("user.dir"))
				.resolve("../missions-design-models")
				.normalize();

		logger.debug("projectPath: {}", projectPath);
		// List<String> fileNames = List.of(
		// "common/messages_def.sysml",
		// "common/zones_def.sysml",
		// "CB/cb_def.sysml",
		// "MPS/mps_def.sysml",
		// "SL/sl_def.sysml",
		// "VGR/vgr_def.sysml",
		// //"CB/cb_missions_def.sysml",
		// //"MPS/mps_missions_def.sysml",
		// "SL/sl_missions_def.sysml"//,
		// //"VGR/vgr_missions_def.sysml"
		// );

		List<String> fileNames = List.of(
				"common/messages_def.sysml",
				"common/zones_def.sysml",
				"TEST-MACHINE/test-machine_def.sysml",
				"TEST-MACHINE/test-machine_missions_def.sysml");

		List<File> files = new ArrayList<File>();

		for (String fileName : fileNames) {

			File sysmlFile = projectPath.resolve(fileName).toFile();
			assertTrue(sysmlFile.exists());
			files.add(ImportUtils.applyFileTransformations(sysmlFile));
		}

		String packagePrefix = "fr.inria.factoryscada.sysmlbaseddomain";

		List<Resource> resources = importer.importSysmlTexts(files, resourceSet);
		assertEquals(resources.size(), fileNames.size());
		assertEquals(resourceSet.getResources().size(), nbLibResources + fileNames.size());

		// Pass 1: Index types
		SymbolIndex.Builder symbolIndexBuilder = new SymbolIndex.Builder();
		IndexerSwitch indexer = new IndexerSwitch(symbolIndexBuilder);

		for (Resource resource : resources) {
			resource.getContents().forEach(indexer::doSwitch);
		}

		SymbolIndex symbolIndex = symbolIndexBuilder.build();

		logger.debug("Symbol Index:\n{}", symbolIndex.toString());

		// Pass 2: Link types and build Intermediate Representation
		IrRepository.Builder irRepositoryBuilder = new IrRepository.Builder();
		ToIrSwitch toIr = new ToIrSwitch(symbolIndex, irRepositoryBuilder);

		for (Resource resource : resources) {
			resource.getContents().forEach(toIr::doSwitch);
		}

		IrRepository irRepository = irRepositoryBuilder.build();

		// Pass 3: Link + Generate Java code
		TypeTable typeTable = new Linker().link(irRepository, packagePrefix);
		JavaTransformer transformer = new JavaTransformer(irRepository, typeTable, packagePrefix);
		Map<String, JavaFile> javaFiles = transformer.generate();

		assertFalse(javaFiles.isEmpty());

		for (Map.Entry<String, JavaFile> entry : javaFiles.entrySet()) {
			logger.info("Generated Java File: {}\n{}", entry.getKey(), entry.getValue());
		}
	}
}
