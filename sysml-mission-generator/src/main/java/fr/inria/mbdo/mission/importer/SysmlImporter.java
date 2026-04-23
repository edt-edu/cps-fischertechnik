package fr.inria.mbdo.mission.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

import fr.inria.mbdo.mission.utils.ImportUtils;


public class SysmlImporter {


	private static final Logger LOGGER = LoggerFactory.getLogger(SysmlImporter.class);
	
	/**
	 * collect error messages to know if everything is ok or not
	 */
	public List<String> errorMessages = new ArrayList<String>(); 
	
	public void clearErrorMessages() {
		errorMessages.clear();
	}
	
	/**
	 * Load the given sysml text into the resourceSet, will create a new resource to contain the sysml model
	 * @param sysmlFile
	 * @param resourceSet
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Resource importSysmlText(File sysmlFileIn, ResourceSet resourceSet) throws FileNotFoundException, IOException {
		Resource result = null;
		SysmlToAst sysmlToAst = new SysmlToAst(null);
        ASTTransformer astTransformer = new ASTTransformer();
        
        // workaround issue in Syson Importer
        LOGGER.warn("Apply Syson importer workaround on {}", sysmlFileIn.getName());
        File sysmlFile = ImportUtils.applyFileTransformations(sysmlFileIn);
        
        try (InputStream inputStream = new FileInputStream(sysmlFile)) {
            AstParsingResult astResult = sysmlToAst.convert(inputStream, "sysml");

            if (!astResult.reports().isEmpty()) {
                final String errorMessage = astResult.reports().stream()
                        .map(Status::toString)
                        .collect(Collectors.joining(System.lineSeparator(), System.lineSeparator(), System.lineSeparator()));
                String msg = "[AST Error] while parsing input file "+ sysmlFile.getAbsolutePath()+ ":\n" + errorMessage;
                errorMessages.add(msg);
                LOGGER.error(msg);
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
                	String msg = "[AST Error]  input file "+ sysmlFile.getAbsolutePath()+ " returned an empty Resource";
                    errorMessages.add(msg);
                    LOGGER.error(msg);
                }
            } else {
            	String msg = "[AST Error]  input file "+ sysmlFile.getAbsolutePath()+ " returned an empty AST";
                errorMessages.add(msg);
                LOGGER.error(msg);
            }
            

            if (!astTransformer.getTransformationMessages().isEmpty()) {
                final String errorMessage = astTransformer.getTransformationMessages().stream()
                        .map(message -> message.level().toString() + " - " + message.body())
                        .collect(Collectors.joining(System.lineSeparator(), System.lineSeparator(), System.lineSeparator()));
                String msg = "[Transformer Error] while transforming input file "+ sysmlFile.getAbsolutePath()+ ":\n" + errorMessage;
                errorMessages.add(msg);
                LOGGER.error(msg);
            }
        }
        return result;
    }
	
	/**
	 * Load the given sysml text files in the provided order. Each file will be located in its own resource
	 * The load order might be important and files should not contains cyclic dependencies
	 * @param sysmlFiles
	 * @param resourceSet
	 * @return the list of created resources
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public List<Resource> importSysmlTexts(List<File> sysmlFiles, ResourceSet resourceSet) throws FileNotFoundException, IOException {
		List<Resource> result = new ArrayList<Resource>();
		for(File sysmlFile : sysmlFiles) {
			result.add(importSysmlText(sysmlFile, resourceSet));
		}
		return result;
	}
	
}
