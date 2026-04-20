package fr.inria.mbdo.mission;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
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
	void generateMachineInterfaces()  throws Exception {
		
		var sysmlFileName = "CBVGRMission/CB.sysml";
		String destDir = tempDir.toFile().getAbsolutePath();
		SysmlMissionGenerator generator = new SysmlMissionGenerator();

		// files from /src/test/resources
		File sysmlFile = new File(getClass().getClassLoader().getResource(sysmlFileName).toURI());
		assertTrue(sysmlFile.exists());
		
		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
        
        System.out.println("Parsing SysML file: " + sysmlFile.getAbsolutePath());

        SysmlImporter importer = new SysmlImporter();
        Resource res= importer.importSysmlText( sysmlFile, resourceSet);
		
		MachineInterfaceGenerator interfaceGenerator = new MachineInterfaceGenerator("fr.inria.factoryscada.sysmlbaseddomain");        
		interfaceGenerator.generate(res.getContents().getFirst()); // TODO deal with multiple root

		interfaceGenerator.getContext().partDefToJavaFile.forEach((k, v) -> {
			logger.info("Generated class for PartDef {}:\n{}", k.effectiveName(), v.toString());
		});
		interfaceGenerator.getContext().enumToJavaFile.forEach((k, v) -> {
			logger.info("Generated class for Enum {}:\n{}", k.effectiveName(), v.toString());
		});
		
	}
}
