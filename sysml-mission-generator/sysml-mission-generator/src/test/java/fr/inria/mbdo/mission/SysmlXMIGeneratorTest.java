package fr.inria.mbdo.mission;

import fr.inria.mbdo.mission.utils.ImportUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysmlXMIGeneratorTest {

    private static final String OUTPUT_DIR = "target/generated-xmi";

    @Test
    void shouldGenerateXmiFilesFromSysml() throws Exception {
        SysmlXMIGenerator generator = new SysmlXMIGenerator();

        List<String> fileNames = List.of(
                "common/common_def.sysml",
                "CB/cb_def.sysml",
                "MPS/mps_def.sysml",
                "SL/sl_def.sysml",
                "VGR/vgr_def.sysml",
                "zones/zones_def.sysml",
                "CB/cb_missions_def.sysml",
                "MPS/mps_missions_def.sysml",
                "SL/sl_missions_def.sysml",
                "VGR/vgr_missions_def.sysml",
                "zones/zones_missions_def.sysml");

        Path projectPath = Paths.get(System.getProperty("user.dir"))
                .resolve("../missions-design-models")
                .normalize();

        File outputDir = ensureOutputDir();
        List<File> generatedFiles = new ArrayList<>();

        for (String fileName : fileNames) {
            File sysmlFile = projectPath.resolve(fileName).toFile();
            assertTrue(sysmlFile.exists(), "Missing input file: " + fileName);

            File outputFile = outputDir.toPath().resolve(fileName + ".xmi").toFile();
            Files.createDirectories(outputFile.toPath().getParent());

            generator.toXmi(ImportUtils.applyFileTransformations(sysmlFile).getAbsolutePath(),
                    outputFile.getAbsolutePath());
            generatedFiles.add(outputFile);
        }

        assertFalse(generatedFiles.isEmpty(), "At least one XMI file should be generated");
        for (File file : generatedFiles) {
            assertTrue(file.exists(), "Missing generated XMI file: " + file.getAbsolutePath());
            assertTrue(Files.size(file.toPath()) > 0,
                    "Generated XMI file should not be empty: " + file.getAbsolutePath());
        }
    }

    private File ensureOutputDir() throws Exception {
        Path outputPath = Paths.get(System.getProperty("user.dir"), OUTPUT_DIR);
        Files.createDirectories(outputPath);
        return outputPath.toFile();
    }
}