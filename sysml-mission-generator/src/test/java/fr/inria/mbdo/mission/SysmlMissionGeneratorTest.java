package fr.inria.mbdo.mission;

import fr.inria.mbdo.mission.importer.SysmlImporter;
import fr.inria.mbdo.mission.utils.ImportUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysmlMissionGeneratorTest {

    private static final Logger logger = LoggerFactory.getLogger(SysmlMissionGeneratorTest.class);
    private static final String OUTPUT_DIR = "target/generated-sysml";

    @Test
    void shouldGenerateFilesFromSysml() throws Exception {
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

        Path projectPath = Paths.get(System.getProperty("user.dir"))
                .resolve("../missions-design-models")
                .normalize();

        List<File> inputFiles = new ArrayList<>();
        for (String fileName : fileNames) {
            File sysmlFile = projectPath.resolve(fileName).toFile();
            assertTrue(sysmlFile.exists(), "Missing input file: " + fileName);
            inputFiles.add(ImportUtils.applyFileTransformations(sysmlFile));
        }

        File outputDir = ensureOutputDir();
        generator.generate(inputFiles, "fr.inria.mbdo.mission.extensions.ren_mission_01", outputDir);

        List<File> generatedFiles = Files.walk(outputDir.toPath())
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .toList();

        assertNotNull(generatedFiles, "Generated files should not be null");
        assertFalse(generatedFiles.isEmpty(), "At least one file should be generated");

        boolean hasNonEmptyFile = false;
        for (File file : generatedFiles) {
            if (Files.size(file.toPath()) > 0) {
                hasNonEmptyFile = true;
                break;
            }
            logger.info("Generated file {} is empty", file.getAbsolutePath());
        }

        assertTrue(hasNonEmptyFile, "At least one generated file should be non-empty");
    }

    @Test
    void shouldCaptureActionsFromVgrTransitions() throws Exception {
        Path projectPath = Paths.get(System.getProperty("user.dir"))
                .resolve("../missions-design-models")
                .normalize();

        List<File> inputFiles = List.of(
                projectPath.resolve("common/common_def.sysml").toFile(),
                projectPath.resolve("common/zones_def.sysml").toFile(),
                projectPath.resolve("common/zones_missions_def.sysml").toFile(),
                projectPath.resolve("CB/cb_def.sysml").toFile(),
                projectPath.resolve("MPS/mps_def.sysml").toFile(),
                projectPath.resolve("SL/sl_def.sysml").toFile(),
                projectPath.resolve("VGR/vgr_def.sysml").toFile(),
                projectPath.resolve("CB/cb_missions_def.sysml").toFile(),
                projectPath.resolve("MPS/mps_missions_def.sysml").toFile(),
                projectPath.resolve("SL/sl_missions_def.sysml").toFile(),
                projectPath.resolve("VGR/vgr_missions_def.sysml").toFile());

        SysmlImporter importer = new SysmlImporter();
        var resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
        ArrayList<Resource> resources = new ArrayList<>(inputFiles.size());
        for (File inputFile : inputFiles) {
            resources.add(importer.importSysmlText(ImportUtils.applyFileTransformations(inputFile),
                    resourceSet));
        }

        var symbolIndexBuilder = new fr.inria.mbdo.mission.generators.SymbolIndex.Builder();
        var indexer = new fr.inria.mbdo.mission.switchs.IndexerSwitch(symbolIndexBuilder);
        for (var resource : resources) {
            resource.getContents().forEach(indexer::doSwitch);
        }

        var irRepositoryBuilder = new fr.inria.mbdo.mission.generators.IrRepository.Builder();
        var toIr = new fr.inria.mbdo.mission.switchs.ToIrSwitch(symbolIndexBuilder.build(),
                irRepositoryBuilder);
        for (var resource : resources) {
            resource.getContents().forEach(toIr::doSwitch);
        }

        var irRepository = irRepositoryBuilder.build();
        var vgrMissions = irRepository.getMissions().values().stream()
                .filter(mission -> mission.getQualifiedName().contains("VacuumGripperMissions"))
                .toList();

        assertFalse(vgrMissions.isEmpty(), "VGR missions should be present in the IR");

        boolean hasTransitionActions = vgrMissions.stream()
                .flatMap(mission -> mission.getStates().stream())
                .map(stateRef -> irRepository.getStates().get(stateRef.qName()))
                .filter(Objects::nonNull)
                .flatMap(state -> state.getTransitions().stream())
                .map(transitionRef -> irRepository.getTransitions().get(transitionRef.qName()))
                .filter(Objects::nonNull)
                .anyMatch(transition -> transition.getAction() != null);

        assertTrue(hasTransitionActions, "VGR transitions should keep their effect actions in the IR");

        vgrMissions.stream()
                .flatMap(mission -> irRepository.getActions().values().stream()
                        .filter(action -> action.getQualifiedName()
                                .contains(mission.getName())));
    }

    @Test
    void shouldRenderSimpleTransitionActionBodiesInJava() throws Exception {
        SysmlMissionGenerator generator = new SysmlMissionGenerator();

        Path projectPath = Paths.get(System.getProperty("user.dir"))
                .resolve("../missions-design-models")
                .normalize();

        List<File> inputFiles = new ArrayList<>();
        for (String fileName : List.of(
                "common/common_def.sysml",
                "common/zones_def.sysml",
                "CB/cb_def.sysml",
                "MPS/mps_def.sysml",
                "SL/sl_def.sysml",
                "VGR/vgr_def.sysml",
                "CB/cb_missions_def.sysml",
                "MPS/mps_missions_def.sysml",
                "SL/sl_missions_def.sysml",
                "VGR/vgr_missions_def.sysml")) {
            File sysmlFile = projectPath.resolve(fileName).toFile();
            assertTrue(sysmlFile.exists(), "Missing input file: " + fileName);
            inputFiles.add(ImportUtils.applyFileTransformations(sysmlFile));
        }

        File outputDir = ensureOutputDir();
        generator.generate(inputFiles, "com.example.generated", outputDir);

        Path generatedMission = outputDir.toPath()
                .resolve("com/example/generated/conveyorbeltmission/ConveyorBeltNominalMission.java");
        assertTrue(Files.exists(generatedMission), "Generated ConveyorBelt mission should exist");

        String missionSource = Files.readString(generatedMission);
        assertTrue(missionSource.contains("CommandSuccessEventMessage.class"),
                "accept Event should compile to a typed event trigger class");
        assertFalse(missionSource.contains("blabla"),
                "Generated triggers should no longer use placeholder expression names");
        // Action bodies are now moved to a generated Actions interface (Javadoc).
        Path actionsInterface = outputDir.toPath()
                .resolve("com/example/generated/conveyorbeltmission/ConveyorBeltNominalMissionActions.java");
        assertTrue(Files.exists(actionsInterface), "Generated ConveyorBelt actions interface should exist");

        String actionsSource = Files.readString(actionsInterface);
        assertTrue(actionsSource.contains("moveToSensor"),
                "Actions Javadoc should contain the conveyor belt method name 'moveToSensor'");
        assertTrue(actionsSource.contains("FeedFreeEventMessage"),
                "Actions Javadoc should reference FeedFreeEventMessage");
        assertTrue(actionsSource.contains("SwapBusyEventMessage"),
                "Actions Javadoc should reference SwapBusyEventMessage");

        Path machineInterface = outputDir.toPath()
                .resolve("com/example/generated/common/Machine.java");
        assertTrue(Files.exists(machineInterface), "Generated Machine interface should exist");

        String machineSource = Files.readString(machineInterface);
        assertTrue(machineSource.contains("if (false)"),
                "Accept-when trigger helpers should use the false-guard placeholder");
        assertFalse(machineSource.contains("ConditionMet()"),
                "Accept-when trigger helpers should not expose a separate boolean condition method");

        // Mission wiring should delegate to the provided actions implementor.
        assertTrue(missionSource.contains("this.actions::"),
                "Mission should delegate runtime actions to actions implementor");

        boolean hasTypedRequestAccept = Files.walk(outputDir.toPath())
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception e) {
                        return "";
                    }
                })
                .anyMatch(source -> source.contains("AcquireRequestEventMessage.class"));
        assertTrue(hasTypedRequestAccept,
                "accept evt : Event should compile to a typed event trigger class");

        Path vgrMission = outputDir.toPath()
                .resolve("com/example/generated/vacuumgrippermissions/VacuumGripper1NominalMission.java");
        assertTrue(Files.exists(vgrMission), "Generated VacuumGripper mission should exist");

        String vgrMissionSource = Files.readString(vgrMission);
        // VacuumGripper action moved to Actions interface Javadoc
        Path vgrActions = outputDir.toPath()
                .resolve("com/example/generated/vacuumgrippermissions/VacuumGripper1NominalMissionActions.java");
        assertTrue(Files.exists(vgrActions), "Generated VacuumGripper actions interface should exist");
        String vgrActionsSource = Files.readString(vgrActions);
        assertTrue(vgrActionsSource.contains("goToPosition"),
                "Actions Javadoc should contain the declared goToPosition perform call");
        assertFalse(vgrMissionSource.contains("VacuumGripper2NominalMission.goToStandby()"),
                "Standby action must not pull in unrelated mission actions");
        assertFalse(vgrMissionSource.contains("vacuumGripper.pick()"),
                "Standby action must not pull in sibling machine actions");
        assertFalse(vgrMissionSource.contains("vacuumGripper.place()"),
                "Standby action must not pull in sibling machine actions");
    }

    private File ensureOutputDir() throws Exception {
        Path outputPath = Paths.get(System.getProperty("user.dir"), OUTPUT_DIR);
        Files.createDirectories(outputPath);
        return outputPath.toFile();
    }
}