package fr.inria.mbdo.mission;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import fr.inria.mbdo.mission.generators.*;
import fr.inria.mbdo.mission.switchs.IndexerSwitch;
import fr.inria.mbdo.mission.switchs.ToIrSwitch;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.palantir.javapoet.JavaFile;

import fr.inria.mbdo.mission.importer.SysmlImporter;

public class SysmlMissionGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysmlMissionGenerator.class);

    public void generate(List<File> inputFiles, String basePackageName, File targetFolder)
            throws FileNotFoundException, IOException {
        LOGGER.debug("generating code for {} sysml files into {}", inputFiles.size(), targetFolder.toPath());
        ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
        SysmlImporter importer = new SysmlImporter();
        List<Resource> resources = importer.importSysmlTexts(inputFiles, resourceSet);

        // Pass 1: Index types
        SymbolIndex.Builder symbolIndexBuilder = new SymbolIndex.Builder();
        IndexerSwitch indexer = new IndexerSwitch(symbolIndexBuilder);

        for (Resource resource : resources) {
            resource.getContents().forEach(indexer::doSwitch);
        }

        SymbolIndex symbolIndex = symbolIndexBuilder.build();

        // Pass 2: Link types and build Intermediate Representation
        IrRepository.Builder irRepositoryBuilder = new IrRepository.Builder();
        ToIrSwitch toIr = new ToIrSwitch(symbolIndex, irRepositoryBuilder);

        for (Resource resource : resources) {
            resource.getContents().forEach(toIr::doSwitch);
        }

        IrRepository irRepository = irRepositoryBuilder.build();

        // Pass 3: Link + Generate Java code
        TypeTable typeTable = new JavaLinker().link(irRepository, basePackageName);
        JavaTransformer transformer = new JavaTransformer(irRepository, typeTable);
        Map<String, JavaFile> javaFiles = transformer.generate();

        writeToFile(javaFiles, targetFolder);
    }

    public void writeToFile(Map<String, JavaFile> javaFiles, File targetFolder) throws IOException {
        for (Map.Entry<String, JavaFile> entry : javaFiles.entrySet()) {
            entry.getValue().writeToFile(targetFolder);
        }
    }

}
