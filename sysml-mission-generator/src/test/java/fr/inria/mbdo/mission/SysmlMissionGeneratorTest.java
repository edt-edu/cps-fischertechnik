package fr.inria.mbdo.mission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SysmlMissionGeneratorTest {

    @TempDir
    File tempDir;

    @Test
    void shouldGenerateFilesFromSysml() throws Exception {
        // given
        SysmlMissionGenerator generator = new SysmlMissionGenerator();

        List<File> inputFiles = TestUtils.getFilesFromSrcResources(
            List.of("MultiFileMission/ConveyorBeltCommands.sysml", "MultiFileMission/ConveyorBelt.sysml") 
        );

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