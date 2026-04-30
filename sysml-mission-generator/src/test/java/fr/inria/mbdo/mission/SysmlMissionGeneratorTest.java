package fr.inria.mbdo.mission;

import fr.inria.mbdo.mission.utils.ImportUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SysmlMissionGeneratorTest {

    Logger logger = LoggerFactory.getLogger(SysmlMissionGeneratorTest.class);

    private static final String OUTPUT_DIR = "target/generated-sysml";

    @Test
    void shouldGenerateFilesFromSysml() throws Exception {
        // given
        SysmlMissionGenerator generator = new SysmlMissionGenerator();

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

        Path projectPath = Paths.get(System.getProperty("user.dir"))
                .resolve("../missions-design-models")
                .normalize();

        List<File> inputFiles = new ArrayList<File>();

        for (String fileName : fileNames) {

            File sysmlFile = projectPath.resolve(fileName).toFile();
            assertTrue(sysmlFile.exists());
            inputFiles.add(ImportUtils.applyFileTransformations(sysmlFile));
        }

        String basePackageName = "com.example.generated";

        // when
        File outputDir = ensureOutputDir();
        generator.generate(inputFiles, basePackageName, outputDir);

        // then
        List<File> allFiles = Files.walk(outputDir.toPath())
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .toList();

        assertNotNull(allFiles, "Generated files should not be null");
        assertFalse(allFiles.isEmpty(), "At least one file should be generated");

        // check file content is not empty
        boolean hasNonEmptyFile = false;
        for (File f : allFiles) {
            if (f.isFile() && Files.size(f.toPath()) > 0) {
                hasNonEmptyFile = true;
                break;
            }
            logger.info("File {}:\n{}", f.getAbsolutePath(), "tmp");
        }

        assertTrue(hasNonEmptyFile, "At least one generated file should be non-empty");
    }

    private File ensureOutputDir() throws IOException {
        Path outputPath = Paths.get(System.getProperty("user.dir"), OUTPUT_DIR);
        Files.createDirectories(outputPath);
        return outputPath.toFile();
    }
}