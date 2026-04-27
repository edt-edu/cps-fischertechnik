package fr.inria.mbdo.mission;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.syson.sysml.ASTTransformer;
import org.eclipse.syson.sysml.AstParsingResult;
import org.eclipse.syson.sysml.Classifier;
import org.eclipse.syson.sysml.SysmlToAst;
import org.eclipse.syson.sysml.textual.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.palantir.javapoet.JavaFile;

import fr.inria.mbdo.mission.generators.MachineInterfaceGenerator;
import fr.inria.mbdo.mission.importer.SysmlImporter;

public class SysmlMissionGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(SysmlMissionGenerator.class);
	
	// TODO deal with multiple files to be loaded
	
	public void generate(String sysmlFilePath, String targetFolderPath) throws FileNotFoundException, IOException {
		File sysmlFile = new File(sysmlFilePath);
        if (!sysmlFile.exists()) {
        	
        	LOGGER.error("File not found: " + sysmlFilePath);
            return;
        }

        File targetFolder = new File(targetFolderPath);
        if (!targetFolder.isDirectory()) {
        	LOGGER.error("target folder must exist: " + targetFolderPath);
            return;
        }
		
        ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
        
        System.out.println("Parsing SysML file: " + sysmlFilePath);

        SysmlImporter importer = new SysmlImporter();
        Resource res= importer.importSysmlText( sysmlFile, resourceSet);
        
        MachineInterfaceGenerator interfaceGenerator = new MachineInterfaceGenerator("fr.inria.factoryscada.sysmlbaseddomain");        
		interfaceGenerator.generate(res.getContents().getFirst()); // TODO deal with multiple root

		writeToFile(interfaceGenerator, targetFolder);
	}
	
	public void generate(List<File> inputFiles, String basePackageName, File targetFolder) throws FileNotFoundException, IOException {

		ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
		SysmlImporter importer = new SysmlImporter();
		List<Resource> resources = importer.importSysmlTexts(inputFiles, resourceSet);
		
		MachineInterfaceGenerator interfaceGenerator = new MachineInterfaceGenerator(basePackageName);
		for (Resource res : resources) {
			interfaceGenerator.generate(res.getContents().getFirst()); // TODO deal with multiple root
		}
		writeToFile(interfaceGenerator, targetFolder);
	}

	public void writeToFile(MachineInterfaceGenerator interfaceGenerator, File targetFolder ) throws IOException {
		for (Entry<Classifier, JavaFile> entry : interfaceGenerator.getContext().classifierToJavaFile.entrySet()) {
			entry.getValue().writeToFile(targetFolder);
		}
	}
	
	
	

}
