package fr.inria.mbdo.mission;

import fr.inria.mbdo.mission.utils.ImportUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-style checks that run the full generator but assert small,
 * focused properties.
 */
class SysmlJavaTransformerIntegrationChecks {

    private static final String OUTPUT_DIR = "target/generated-sysml";

    @Test
    void generatedActionsInterfaceExistsForConveyorBelt() throws Exception {
        SysmlMissionGenerator generator = new SysmlMissionGenerator();

        List<String> fileNames = List.of(
                "common/common_def.sysml",
                "zones/zones_def.sysml",
                "CB/cb_def.sysml",
                "MPS/mps_def.sysml",
                "SL/sl_def.sysml",
                "VGR/vgr_def.sysml",
                "zones/zones_missions_def.sysml",
                "CB/cb_missions_def.sysml",
                "MPS/mps_missions_def.sysml",
                "SL/sl_missions_def.sysml",
                "VGR/vgr_missions_def.sysml");

        Path projectPath = Paths.get(System.getProperty("user.dir")).resolve("../missions-design-models").normalize();

        List<File> inputFiles = new ArrayList<>();
        for (String fileName : fileNames) {
            inputFiles.add(ImportUtils.applyFileTransformations(projectPath.resolve(fileName).toFile()));
        }

        File outputDir = Paths.get(System.getProperty("user.dir"), OUTPUT_DIR).toFile();
        outputDir.mkdirs();
        generator.generate(inputFiles, "com.example.generated", outputDir);

        Path actionsInterface = outputDir.toPath()
                .resolve("com/example/generated/conveyorbeltmissions/conveyorbeltnominalmission/ConveyorBeltNominalMissionActions.java");
        assertTrue(Files.exists(actionsInterface), "Generated actions interface for conveyor belt should exist");

        Path abstractAdapter = outputDir.toPath()
                .resolve("com/example/generated/conveyorbeltsystem/conveyorbelt/AbstractConveyorBeltMachineAdapter.java");
        assertTrue(Files.exists(abstractAdapter), "Generated abstract adapter for ConveyorBelt should exist");
    }

    @Test
    void missionDelegatesToActionsImplementor() throws Exception {
        SysmlMissionGenerator generator = new SysmlMissionGenerator();

        List<String> fileNames = List.of(
                "common/common_def.sysml",
                "zones/zones_def.sysml",
                "CB/cb_def.sysml",
                "MPS/mps_def.sysml",
                "SL/sl_def.sysml",
                "VGR/vgr_def.sysml",
                "zones/zones_missions_def.sysml",
                "CB/cb_missions_def.sysml",
                "MPS/mps_missions_def.sysml",
                "SL/sl_missions_def.sysml",
                "VGR/vgr_missions_def.sysml");

        Path projectPath = Paths.get(System.getProperty("user.dir")).resolve("../missions-design-models").normalize();

        List<File> inputFiles = new ArrayList<>();
        for (String fileName : fileNames) {
            inputFiles.add(ImportUtils.applyFileTransformations(projectPath.resolve(fileName).toFile()));
        }

        File outputDir = Paths.get(System.getProperty("user.dir"), OUTPUT_DIR).toFile();
        outputDir.mkdirs();
        generator.generate(inputFiles, "com.example.generated", outputDir);

        Path mission = outputDir.toPath()
                .resolve("com/example/generated/conveyorbeltmissions/conveyorbeltnominalmission/ConveyorBeltNominalMission.java");
        String missionSource = Files.readString(mission);
        assertTrue(missionSource.contains("this.actions."),
                "Mission should delegate runtime actions to actions implementor");
    }
}
