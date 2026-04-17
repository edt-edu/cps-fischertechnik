package fr.inria.mbdo.mission;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.syson.sysml.ASTTransformer;
import org.eclipse.syson.sysml.AstParsingResult;
import org.eclipse.syson.sysml.SysmlToAst;
import org.eclipse.syson.sysml.textual.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.mbdo.mission.generators.MachineInterfaceGenerator;

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

        Resource res= this.importSysml( sysmlFile, resourceSet);
        
        MachineInterfaceGenerator interfaceGenerator = new MachineInterfaceGenerator("fr.inria.factoryscada.sysmlbaseddomain");        
		interfaceGenerator.generate(res.getContents().getFirst()); // TODO deal with multiple root
        
	}
	
	public Resource importSysml(File sysmlFile, ResourceSet resourceSet) throws FileNotFoundException, IOException {
		Resource result = null;
		SysmlToAst sysmlToAst = new SysmlToAst(null);
        ASTTransformer astTransformer = new ASTTransformer();
        try (InputStream inputStream = new FileInputStream(sysmlFile)) {
            AstParsingResult astResult = sysmlToAst.convert(inputStream, "sysml");

            if (!astResult.reports().isEmpty()) {
                final String errorMessage = astResult.reports().stream()
                        .map(Status::toString)
                        .collect(Collectors.joining(System.lineSeparator(), System.lineSeparator(), System.lineSeparator()));
                LOGGER.error("[AST] while parsing input file : " + errorMessage);
            }

            if (astResult.ast().isPresent()) {
                Resource resource = astTransformer.convertResource(astResult.ast().get(), resourceSet);

                if (resource != null && !resource.getContents().isEmpty()) {                	                	
                	resource.setURI(URI.createURI("sysml://"+sysmlFile.getCanonicalPath()));
                	LOGGER.info("Model parsed successfully.");
                	result = resource;
//                    XMIResource resourceToSave = new XMIResourceImpl(URI.createFileURI(targetFilePath));
//                    resourceToSave.getContents().addAll(resource.getContents());
//                    resourceToSave.save(Collections.emptyMap());
                } else {
                	LOGGER.error("Failed to parse resource or resource is empty.");
                }
            } else {
            	LOGGER.error("Failed convert resource. Returned an empty AST");
            }
            

            if (!astTransformer.getTransformationMessages().isEmpty()) {
                final String errorMessage = astTransformer.getTransformationMessages().stream()
                        .map(message -> message.level().toString() + " - " + message.body())
                        .collect(Collectors.joining(System.lineSeparator(), System.lineSeparator(), System.lineSeparator()));
                LOGGER.error("Error while parsing input file : " + errorMessage);
            }
        }
        return result;
    }
	

}
