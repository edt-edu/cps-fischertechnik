package fr.inria.mbdo.mission;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.syson.sysml.Classifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.palantir.javapoet.JavaFile;

import com.palantir.javapoet.JavaFile;

import fr.inria.mbdo.mission.generators.SysmlAstDotGenerator;
import fr.inria.mbdo.mission.importer.SysmlImporter;

public class SysmlMissionGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysmlMissionGenerator.class);

    public void generate(List<File> inputFiles, String basePackageName, File targetFolder)
            throws FileNotFoundException, IOException {
        LOGGER.debug("generating code for {} sysml files into {}", inputFiles.size(), targetFolder.toPath());
        ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
        SysmlImporter importer = new SysmlImporter();
        List<Resource> resources = importer.importSysmlTexts(inputFiles, resourceSet);

        MachineInterfaceGenerator interfaceGenerator = new MachineInterfaceGenerator(basePackageName);
        for (Resource res : resources) {
            interfaceGenerator.generate(res.getContents().getFirst()); // TODO deal with multiple root
        }
        writeToFile(interfaceGenerator, targetFolder);
    }

    public void writeToFile(MachineInterfaceGenerator interfaceGenerator, File targetFolder) throws IOException {
        for (Entry<Classifier, JavaFile> entry : interfaceGenerator.getContext().classifierToJavaFile.entrySet()) {
            entry.getValue().writeToFile(targetFolder);
        }
    }

}
