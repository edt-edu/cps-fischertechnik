package fr.inria.mbdo.mission;

import fr.inria.mbdo.mission.generators.IrRepository;
import fr.inria.mbdo.mission.generators.JavaLinker;
import fr.inria.mbdo.mission.generators.JavaTransformer;
import fr.inria.mbdo.mission.importer.SysmlImporter;
import fr.inria.mbdo.mission.utils.ImportUtils;
import fr.inria.mbdo.mission.generators.SymbolIndex;
import fr.inria.mbdo.mission.switchs.IndexerSwitch;
import fr.inria.mbdo.mission.switchs.ToIrSwitch;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Small focused tests for JavaTransformer outputs (Actions interface + mission
 * wiring).
 * These tests use a minimal subset of SysML model files to exercise generation
 * logic
 * without running the full integration test suite.
 */
class SysmlJavaTransformerUnitTest {

    @Test
    void shouldGenerateActionsInterfaceAndMissionWiring() throws Exception {
        Path projectPath = Paths.get(System.getProperty("user.dir")).resolve("../missions-design-models").normalize();

        // Choose a small subset that contains conveyor belt mission and the referenced
        // vacuum gripper
        List<File> inputFiles = List.of(
                projectPath.resolve("common/common_def.sysml").toFile(),
                projectPath.resolve("CB/cb_def.sysml").toFile(),
                projectPath.resolve("CB/cb_missions_def.sysml").toFile(),
                projectPath.resolve("VGR/vgr_def.sysml").toFile(),
                projectPath.resolve("VGR/vgr_missions_def.sysml").toFile());

        SysmlImporter importer = new SysmlImporter();
        var resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
        ArrayList<Resource> resources = new ArrayList<>(inputFiles.size());
        for (File inputFile : inputFiles) {
            resources.add(importer.importSysmlText(ImportUtils.applyFileTransformations(inputFile), resourceSet));
        }

        var symbolIndexBuilder = new SymbolIndex.Builder();
        var indexer = new IndexerSwitch(symbolIndexBuilder);
        for (var resource : resources) {
            resource.getContents().forEach(indexer::doSwitch);
        }

        var irRepositoryBuilder = new IrRepository.Builder();
        var toIr = new ToIrSwitch(symbolIndexBuilder.build(), irRepositoryBuilder);
        for (var resource : resources) {
            resource.getContents().forEach(toIr::doSwitch);
        }

        var irRepository = irRepositoryBuilder.build();
        var typeTable = new JavaLinker().link(irRepository, "com.example.generated");

        JavaTransformer transformer = new JavaTransformer(irRepository, typeTable);
        var generated = transformer.generate();

        // Expect an Actions interface for ConveyorBeltNominalMission
        boolean hasActions = generated.keySet().stream().anyMatch(k -> k.contains("ConveyorBeltNominalMissionActions"));
        assertTrue(hasActions, "Generated outputs should include a ConveyorBeltNominalMissionActions interface");

        // Expect the mission class to contain delegation wiring (checked in generated
        // source string)
        var missionEntry = generated.entrySet().stream()
                .filter(e -> e.getKey().contains("ConveyorBeltNominalMission.java")
                        || e.getKey().contains("ConveyorBeltNominalMission"))
                .findFirst();
        if (missionEntry.isPresent()) {
            String src = missionEntry.get().getValue().toString();
            assertTrue(src.contains("this.actions::") || src.contains("actions::"),
                    "Mission source should delegate runtime actions to actions implementor");
        }
    }
}
