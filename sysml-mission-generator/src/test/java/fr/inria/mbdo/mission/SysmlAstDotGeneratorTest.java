package fr.inria.mbdo.mission;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import fr.inria.mbdo.mission.utils.ImportUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.Test;

import fr.inria.mbdo.mission.generators.SysmlAstDotGenerator;
import fr.inria.mbdo.mission.importer.SysmlImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SysmlAstDotGeneratorTest {
    private static final Logger logger = LoggerFactory.getLogger(SysmlAstDotGeneratorTest.class);

    @Test
    void generateAstDotFile() throws Exception {
        SysmlImporter importer = new SysmlImporter();
        Path persistedOutputRoot = Paths.get(System.getProperty("user.dir"), "target", "generated-ast-dot-tests");
        Files.createDirectories(persistedOutputRoot);

        ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);

        // files from ../missions-design-models relatively to the folder containing
        // pom.xml
        Path projectPath = Paths.get(System.getProperty("user.dir"))
                .resolve("../missions-design-models")
                .normalize();

        List<String> fileNames = List.of(
                "common/messages_def.sysml",
                "common/zones_def.sysml",
//                "CB/cb_def.sysml",
//                "MPS/mps_def.sysml",
//                "SL/sl_def.sysml",
//                "VGR/vgr_def.sysml",
//                "CB/cb_missions_def.sysml",
//                "MPS/mps_missions_def.sysml",
//                "SL/sl_missions_def.sysml",
                "VGR/vgr_missions_def.sysml",
                "TEST-MACHINE/test-machine_def.sysml",
                "TEST-MACHINE/test-machine_missions_def.sysml"
        );
        for (String fileName : fileNames) {

            File sysmlFile = projectPath.resolve(fileName).toFile();
            assertTrue(sysmlFile.exists());

            Resource resource = importer.importSysmlText(ImportUtils.applyFileTransformations(sysmlFile), resourceSet);

            Path outputFile = persistedOutputRoot.resolve(fileName + "-ast.dot");
            Path generatedFile = new SysmlAstDotGenerator().generate(resource.getContents().getFirst(), outputFile);
            String dot = Files.readString(generatedFile);

            assertFalse(dot.contains("OwningMembership"));

            logger.info("Generated DOT for {} at {}:\n\n{}", fileName, outputFile, dot);
        }
    }
}