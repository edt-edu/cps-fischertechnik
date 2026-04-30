package fr.inria.mbdo.mission;

import com.palantir.javapoet.JavaFile;
import fr.inria.mbdo.mission.generators.*;
import fr.inria.mbdo.mission.importer.SysmlImporter;
import fr.inria.mbdo.mission.switchs.IndexerSwitch;
import fr.inria.mbdo.mission.switchs.ToIrSwitch;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class SysmlAbstractSyntaxTreeGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(SysmlAbstractSyntaxTreeGenerator.class);
	private static final int DEFAULT_MAX_DEPTH = 20;

	private final DotTraversalSwitch traversalSwitch = new DotTraversalSwitch();

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
		TypeTable typeTable = new Linker().link(irRepository, basePackageName);
		JavaTransformer transformer = new JavaTransformer(irRepository, typeTable, basePackageName);
		Map<String, JavaFile> javaFiles = transformer.generate();

		writeToFile(javaFiles, targetFolder);
	}

	public Path generateAstDot(EObject rootSource, Path outputFile) throws IOException {
		return generateAstDot(rootSource, outputFile, DEFAULT_MAX_DEPTH);
	}

	public Path generateAstDot(EObject rootSource, Path outputFile, int maxDepth) throws IOException {
		String dot = generateAstDot(rootSource, maxDepth);
		Path parent = outputFile.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Files.writeString(outputFile, dot, StandardCharsets.UTF_8);
		LOGGER.info("Wrote SysML AST dot file to {}", outputFile);
		return outputFile;
	}

	public String generateAstDot(EObject rootSource, int maxDepth) {
		traversalSwitch.reset(Math.max(0, maxDepth));
		traversalSwitch.doSwitch(rootSource);
		return traversalSwitch.buildDot();
	}

	public void writeToFile(Map<String, JavaFile> javaFiles, File targetFolder) throws IOException {
		for (Entry<String, JavaFile> entry : javaFiles.entrySet()) {
			entry.getValue().writeToFile(targetFolder);
		}
	}

	private static String escapeDotLabel(String value) {
		return value.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\r", "")
				.replace("\n", "\\n");
	}

	private static String buildLabel(EObject object) {
		StringBuilder label = new StringBuilder(object.eClass().getName());
		if (object instanceof Element element) {
			String qualifiedName = element.getQualifiedName();
			if (qualifiedName != null && !qualifiedName.isBlank()) {
				label.append("\\n").append(qualifiedName);
			} else if (element.getName() != null && !element.getName().isBlank()) {
				label.append("\\n").append(element.getName());
			}
		}
		return label.toString();
	}

	private static boolean isOwningMembership(EObject object) {
		return "OwningMembership".equals(object.eClass().getName());
	}

	private final class DotTraversalSwitch extends SysmlSwitch<Void> {
		private final Map<EObject, String> nodeIds = new IdentityHashMap<>();
		private final Map<EObject, Boolean> visitedNodes = new IdentityHashMap<>();
		private final Deque<String> parentStack = new ArrayDeque<>();
		private final StringBuilder nodeDeclarations = new StringBuilder();
		private final StringBuilder edgeDeclarations = new StringBuilder();
		private final AtomicInteger nextNodeId = new AtomicInteger();
		private int maxDepth;

		void reset(int maxDepth) {
			nodeIds.clear();
			visitedNodes.clear();
			parentStack.clear();
			nodeDeclarations.setLength(0);
			edgeDeclarations.setLength(0);
			nextNodeId.set(0);
			this.maxDepth = maxDepth;
		}

		String buildDot() {
			return "digraph SysmlAst {\n"
					+ "  rankdir=LR;\n"
					+ "  graph [splines=true, overlap=false, bgcolor=\"white\"];\n"
					+ "  node [shape=box, style=\"rounded,filled\", fillcolor=\"#f8f9fb\", color=\"#4b5563\", fontname=\"Helvetica\"];\n"
					+ "  edge [color=\"#6b7280\"];\n\n"
					+ nodeDeclarations
					+ edgeDeclarations
					+ "}\n";
		}

		private void visit(EObject object) {
			if (isOwningMembership(object)) {
				if (visitedNodes.putIfAbsent(object, Boolean.TRUE) != null) {
					return;
				}
				int currentDepth = parentStack.size();
				if (currentDepth >= maxDepth) {
					return;
				}
				for (EObject child : object.eContents()) {
					doSwitch(child);
				}
				return;
			}

			String nodeId = nodeIds.computeIfAbsent(object, key -> {
				String generatedId = "n" + nextNodeId.getAndIncrement();
				nodeDeclarations.append("  ")
						.append(generatedId)
						.append(" [label=\"")
						.append(escapeDotLabel(buildLabel(key)))
						.append("\"];\n");
				return generatedId;
			});

			if (!parentStack.isEmpty()) {
				edgeDeclarations.append("  ")
						.append(parentStack.peek())
						.append(" -> ")
						.append(nodeId)
						.append(";\n");
			}

			if (visitedNodes.putIfAbsent(object, Boolean.TRUE) != null) {
				return;
			}

			int currentDepth = parentStack.size();
			if (currentDepth >= maxDepth) {
				return;
			}

			parentStack.push(nodeId);
			for (EObject child : object.eContents()) {
				doSwitch(child);
			}
			parentStack.pop();
		}

		@Override
		public Void casePackage(Package object) {
			visit(object);
			return null;
		}

		@Override
		public Void caseElement(Element object) {
			visit(object);
			return null;
		}

		@Override
		public Void defaultCase(EObject object) {
			visit(object);
			return null;
		}
	}

}
