package fr.inria.mbdo.mission;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import fr.inria.mbdo.mission.generators.GlobalGenerator;
import fr.inria.mbdo.mission.generators.TransformationContext;
import org.aspectj.weaver.tools.Trace;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.mbdo.mission.generators.SysmlAstDotGenerator;
import fr.inria.mbdo.mission.importer.SysmlImporter;

public class SysmlMissionGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysmlMissionGenerator.class);
    private static final int AST_DOT_MAX_DEPTH = 4;

    // TODO deal with multiple files to be loaded

    public void generate(String sysmlFilePath, String targetFolderPath) throws FileNotFoundException, IOException {
        File sysmlFile = new File(sysmlFilePath);
        if (!sysmlFile.exists()) {

            LOGGER.error("File not found: " + sysmlFilePath);
            return;
        }

        File targetFolder = new File(targetFolderPath);
        if (!targetFolder.exists()) {
            Files.createDirectories(targetFolder.toPath());
        }
        if (!targetFolder.isDirectory()) {
            LOGGER.error("target folder must be a directory: " + targetFolderPath);
            return;
        }

        ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);

        System.out.println("Parsing SysML file: " + sysmlFilePath);

        SysmlImporter importer = new SysmlImporter();
        Resource res = importer.importSysmlText(sysmlFile, resourceSet);

        TransformationContext context = new TransformationContext("fr.inria.factoryscada.sysmlbaseddomain");

        GlobalGenerator interfaceGenerator = new GlobalGenerator(context);
        interfaceGenerator.generate(res.getContents().getFirst()); // TODO deal with multiple roots

        String dotFileName = sysmlFile.getName().replaceFirst("\\.[^.]+$", "") + ".dot";
        new SysmlAstDotGenerator().generate(res.getContents().getFirst(), targetFolder.toPath().resolve(dotFileName),
                AST_DOT_MAX_DEPTH);

    }

}
