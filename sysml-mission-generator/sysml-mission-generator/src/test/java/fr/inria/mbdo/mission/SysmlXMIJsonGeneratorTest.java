package fr.inria.mbdo.mission;

import fr.inria.mbdo.mission.utils.ImportUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysmlXMIJsonGeneratorTest {

    private static final String XMI_OUTPUT_DIR = "target/generated-xmi";
    private static final String JSON_OUTPUT_DIR = "target/generated-json";

    @Test
    void shouldGenerateJsonFromXmi() throws Exception {
        Path projectPath = Paths.get(System.getProperty("user.dir"))
                .resolve("../missions-design-models")
                .normalize();

        File sourceSysmlFile = projectPath.resolve("SL/sl_missions_def.sysml").toFile();
        assertTrue(sourceSysmlFile.exists(), "Missing input file: SL/sl_missions_def.sysml");

        File xmiOutputFile = Paths.get(System.getProperty("user.dir"), XMI_OUTPUT_DIR,
                "SL/sl_missions_def.sysml.xmi").toFile();
        Files.createDirectories(xmiOutputFile.toPath().getParent());
        new SysmlXMIGenerator().toXmi(ImportUtils.applyFileTransformations(sourceSysmlFile).getAbsolutePath(),
                xmiOutputFile.getAbsolutePath());

        File jsonOutputFile = Paths.get(System.getProperty("user.dir"), JSON_OUTPUT_DIR,
                "SL/sl_missions_def.sysml.json").toFile();
        Files.createDirectories(jsonOutputFile.toPath().getParent());
        new SysmlXMIJsonGenerator().toJson(xmiOutputFile.getAbsolutePath(), jsonOutputFile.getAbsolutePath());

        assertTrue(jsonOutputFile.exists(), "Generated JSON file should exist");
        String json = Files.readString(jsonOutputFile.toPath());

        assertTrue(json.contains("sysml:Namespace"), "JSON should keep the SysML root type");
        assertTrue(json.contains("sysml:Package"), "JSON should keep typed nested elements");
        assertTrue(json.contains("sysml:StateDefinition"), "JSON should keep typed state definitions");
        assertTrue(json.contains("sysml:Documentation"), "JSON should keep typed documentation elements");
        assertFalse(json.contains("ownedRelationship"), "JSON should not use ownedRelationship tags");
        assertFalse(json.contains("ownedRelatedElement"), "JSON should not use ownedRelatedElement tags");
    }
}