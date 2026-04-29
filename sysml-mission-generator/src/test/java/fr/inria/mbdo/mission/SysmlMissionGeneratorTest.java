package fr.inria.mbdo.mission;

import fr.inria.mbdo.mission.utils.ImportUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SysmlMissionGeneratorTest {

    @TempDir
    File tempDir;

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
        generator.generate(inputFiles, basePackageName, tempDir);

        // then
        List<File> allFiles = Files.walk(tempDir.toPath())
        	    .filter(Files::isRegularFile)
        	    .map(path -> path.toFile())
        	    .toList();

        assertNotNull(allFiles, "Generated files should not be null");
        assertTrue(allFiles.size() > 0, "At least one file should be generated");

        // check file content is not empty
        boolean hasNonEmptyFile = false;
        for (File f : allFiles) {
            if (f.isFile() && Files.size(f.toPath()) > 0) {
                hasNonEmptyFile = true;
                break;
            }
        }

        assertTrue(hasNonEmptyFile, "At least one generated file should be non-empty");
    }
}