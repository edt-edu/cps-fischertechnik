package fr.inria.mbdo.mission;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.syson.sysml.util.SysmlResourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.syson.sysml.ASTTransformer;
import org.eclipse.syson.sysml.AstParsingResult;
import org.eclipse.syson.sysml.SysmlToAst;
import org.eclipse.syson.sysml.textual.utils.Status;

public class SysmlMissionGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(SysmlMissionGenerator.class);
	
	public void generate(String sysmlFilePath, String targetFolderPath) throws FileNotFoundException, IOException {
		File sysmlFile = new File(sysmlFilePath);
        if (!sysmlFile.exists()) {
        	
            System.err.println("File not found: " + sysmlFilePath);
            return;
        }

        File targetFolder = new File(targetFolderPath);
        if (!targetFolder.isDirectory()) {
            System.err.println("target folder must exist: " + targetFolderPath);
            return;
        }
		
        ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
        
        System.out.println("Parsing SysML file: " + sysmlFilePath);

        this.importSysml( sysmlFile, resourceSet);
        
	}
	
	public void importSysml(File sysmlFile, ResourceSet resourceSet) throws FileNotFoundException, IOException {
		SysmlToAst sysmlToAst = new SysmlToAst(null);
        ASTTransformer astTransformer = new ASTTransformer();
        try (InputStream inputStream = new FileInputStream(sysmlFile)) {
            AstParsingResult astResult = sysmlToAst.convert(inputStream, "sysml");

            if (!astResult.reports().isEmpty()) {
                final String errorMessage = astResult.reports().stream()
                        .map(Status::toString)
                        .collect(Collectors.joining(System.lineSeparator(), System.lineSeparator(), System.lineSeparator()));
                System.err.println("[AST] while parsing input file : " + errorMessage);
            }

            if (astResult.ast().isPresent()) {
                Resource resource = astTransformer.convertResource(astResult.ast().get(), resourceSet);

                if (resource != null && !resource.getContents().isEmpty()) {
                    System.out.println("Model parsed successfully.");
//                    XMIResource resourceToSave = new XMIResourceImpl(URI.createFileURI(targetFilePath));
//                    resourceToSave.getContents().addAll(resource.getContents());
//                    resourceToSave.save(Collections.emptyMap());
                } else {
                    System.err.println("Failed to parse resource or resource is empty.");
                }
            } else {
            	System.err.println("Failed convert resource. Returned an empty AST");
            }
            

            if (!astTransformer.getTransformationMessages().isEmpty()) {
                final String errorMessage = astTransformer.getTransformationMessages().stream()
                        .map(message -> message.level().toString() + " - " + message.body())
                        .collect(Collectors.joining(System.lineSeparator(), System.lineSeparator(), System.lineSeparator()));
                System.err.println("Error while parsing input file : " + errorMessage);
            }
        }
    }
	

}
