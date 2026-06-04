package fr.inria.mbdo.mission;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.syson.sysml.ASTTransformer;
import org.eclipse.syson.sysml.AstParsingResult;
import org.eclipse.syson.sysml.SysmlToAst;
import org.eclipse.syson.sysml.textual.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysmlXMIGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysmlXMIGenerator.class);

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            LOGGER.error("""
                    Usage: SysmlXMIGenerator <sysmlFilePath> <targetFilePath>
                        <sysmlFilePath> : Path to the SysML file
                        <targetFilePath> : Path to output file
                    """);
            return;
        }

        String sysmlFilePath = args[0];
        String targetFilePath = args[1];
        new SysmlXMIGenerator().toXmi(sysmlFilePath, targetFilePath);
    }

    public void toXmi(String sysmlFilePath, String targetFilePath) throws IOException {
        File sysmlFile = new File(sysmlFilePath);
        if (!sysmlFile.exists()) {
            LOGGER.error("File not found: {}", sysmlFilePath);
            return;
        }

        final Path targetPath = Path.of(targetFilePath);
        if (!targetPath.getFileName().toString().endsWith(".xmi")) {
            LOGGER.error("That target file should have the extension .xmi: {}", targetFilePath);
            return;
        }

        ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);

        LOGGER.info("Parsing SysML file: {}", sysmlFilePath);

        this.convert(targetFilePath, sysmlFile, resourceSet);

        this.addHeader(targetFilePath, sysmlFile);
    }

    private void addHeader(String targetFilePath, File sysmlFile) throws IOException {
        String sysMLFileContent = Files.readString(sysmlFile.toPath(), StandardCharsets.UTF_8);
        String comment = new StringBuilder()
                .append("Model generated from a .sysml file with ").append(SysmlXMIGenerator.class.getName())
                .append(" application.").append(System.lineSeparator())
                .append("Original model : ").append(System.lineSeparator())
                .append(sysMLFileContent).toString();

        new XMLHeaderWriter().writeHeader(comment, targetFilePath);
    }

    private void convert(String targetFilePath, File sysmlFile, ResourceSet resourceSet) throws IOException {
        SysmlToAst sysmlToAst = new SysmlToAst(null);
        ASTTransformer astTransformer = new ASTTransformer();
        try (InputStream inputStream = new FileInputStream(sysmlFile)) {
            AstParsingResult astResult = sysmlToAst.convert(inputStream, "sysml");

            if (!astResult.reports().isEmpty()) {
                final String errorMessage = astResult.reports().stream()
                        .map(Status::toString)
                        .collect(Collectors.joining(System.lineSeparator(), System.lineSeparator(),
                                System.lineSeparator()));
                LOGGER.error("[AST] while parsing input file : {}", errorMessage);
            }

            var ast = astResult.ast().orElse(null);
            if (ast == null) {
                LOGGER.error("Failed to parse SysML AST from input file: {}", sysmlFile.getAbsolutePath());
            } else {
                Resource resource = astTransformer.convertResource(ast, resourceSet);

                if (resource != null && !resource.getContents().isEmpty()) {
                    LOGGER.info("Model parsed successfully.");
                    XMIResource resourceToSave = new XMIResourceImpl(URI.createFileURI(targetFilePath));
                    resourceToSave.getContents().addAll(resource.getContents());
                    resourceToSave.save(Collections.emptyMap());
                } else {
                    LOGGER.error("Failed to parse resource or resource is empty.");
                }
            }

            if (!astTransformer.getTransformationMessages().isEmpty()) {
                final String errorMessage = astTransformer.getTransformationMessages().stream()
                        .map(message -> message.level().toString() + " - " + message.body())
                        .collect(Collectors.joining(System.lineSeparator(), System.lineSeparator(),
                                System.lineSeparator()));
                LOGGER.error("Error while parsing input file : {}", errorMessage);
            }
        }
    }
}

class XMLHeaderWriter {

    void writeHeader(String comment, String targetFilePath) throws IOException {
        Path targetPath = Path.of(targetFilePath);
        String targetContent = Files.readString(targetPath, StandardCharsets.UTF_8);
        String normalizedComment = sanitizeForXmlComment(comment);
        String header = "<!-- " + normalizedComment + " -->" + System.lineSeparator();

        int xmlDeclarationEnd = targetContent.indexOf(System.lineSeparator());
        String contentWithHeader;
        if (xmlDeclarationEnd >= 0 && targetContent.startsWith("<?xml")) {
            int insertionPoint = xmlDeclarationEnd + System.lineSeparator().length();
            contentWithHeader = targetContent.substring(0, insertionPoint) + header
                    + targetContent.substring(insertionPoint);
        } else {
            contentWithHeader = header + targetContent;
        }

        Files.writeString(targetPath, contentWithHeader, StandardCharsets.UTF_8);
    }

    private String sanitizeForXmlComment(String comment) {
        return comment.replace("--", "- -");
    }
}
