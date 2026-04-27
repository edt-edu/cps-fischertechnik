package fr.inria.mbdo.mission.generators;

import static fr.inria.mbdo.mission.utils.StringUtils.toUpperFirst;
import static fr.inria.mbdo.mission.utils.SymlUtils.getParentJavaPackageQualifiedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.ActionDefinition;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.AttributeUsage;
import org.eclipse.syson.sysml.Classifier;
import org.eclipse.syson.sysml.Documentation;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.EnumerationDefinition;
import org.eclipse.syson.sysml.EnumerationUsage;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.PerformActionUsage;
import org.eclipse.syson.sysml.Type;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

public class MachineInterfaceGenerator {
	private static final Logger logger = LoggerFactory.getLogger(MachineInterfaceGenerator.class);

	String packagePrefix;
	MachineInterfaceGeneratorSwitch traversalSwitch;
	SymlToJavaTypeSwitch typeSwitch;

	TransformationContext context;

	public TransformationContext getContext() {
		return context;
	}

	public MachineInterfaceGenerator(String packagePrefix) {
		this.context = new TransformationContext();
		this.traversalSwitch = new MachineInterfaceGeneratorSwitch(context);
		this.typeSwitch = new SymlToJavaTypeSwitch(packagePrefix);
		this.packagePrefix = packagePrefix;
	}

	/**
	 * class used to store the generator context ie. element to be printed in the
	 * end note: depending on the transformation/code gen it can be replaced by the
	 * return type of the do switch
	 */
	public class TransformationContext {

		/**
		 * map containing the sysml element as key and the created JavaFile
		 */
		public Map<Classifier, JavaFile> classifierToJavaFile = new HashMap<>();
		//public Map<EnumerationDefinition, JavaFile> enumToJavaFile = new HashMap<>();

		/**
		 * list of indirect type used in the element that need to be generated too
		 * 
		 */
		public List<Type> indirectTypesToGenerate = new ArrayList<Type>();
	}

	public class MachineInterfaceGeneratorSwitch extends SysmlSwitch<List<String>> {
		public TransformationContext transformationContext;

		public MachineInterfaceGeneratorSwitch(TransformationContext transformationContext) {
			this.transformationContext = transformationContext;

		}

		@Override
		public List<String> casePackage(Package object) {
			List<String> result = new ArrayList<>();
			logger.debug("traversing Package " + object.getName());
			result.addAll(doSwitchForAllOwnedElements(object)); // look into children, as this is a package
			return result;
		}

		@Override
		public List<String> casePartDefinition(PartDefinition object) {
			List<String> result = new ArrayList<>();
			logger.debug("traversing PartDefinition {}", object.getName());

			TypeSpec.Builder partDefInterfaceBuilder = TypeSpec.interfaceBuilder(object.getName())
					.addModifiers(Modifier.PUBLIC)
					.addJavadoc("From $L\n$L", object.getQualifiedName(), getDocString(object.getDocumentation()));

			for (AttributeUsage ownedAttribute : object.getOwnedAttribute()) {

				// attribute type

				MethodSpec.Builder getterBuilder = MethodSpec
						.methodBuilder("get" + toUpperFirst(ownedAttribute.getName()))
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
				logger.debug("computing Java type for attribute {}", ownedAttribute.getQualifiedName());
				if (ownedAttribute.getType().isEmpty()) {
					logger.error("Missing attribute type for {}", ownedAttribute.getQualifiedName());
				}
				if (ownedAttribute.getType().size() > 1) {
					logger.error("Multiple type attribute not supported for {}", ownedAttribute.getQualifiedName());
				}
				Type attributeType = ownedAttribute.getType().getFirst();
				// possibly asks to generate the java class for the type
				addIndirectTypesToGenerate(attributeType);
				TypeName typeName = typeSwitch.doSwitch(attributeType);
				getterBuilder.returns(typeName);

				MethodSpec.Builder setterBuilder = MethodSpec
						.methodBuilder("set" + toUpperFirst(ownedAttribute.getName()))
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.addParameter(typeName, ownedAttribute.getName());

				partDefInterfaceBuilder.addMethod(setterBuilder.build());
				partDefInterfaceBuilder.addMethod(getterBuilder.build());
			}

			for (ActionUsage action : object.getOwnedAction()) {
				if (action instanceof PerformActionUsage) {
					PerformActionUsage pau = (PerformActionUsage) action;
					logger.error(pau.getType().toString());
					if (pau.getType().size() == 0) {
						logger.error("Missing Action definition for PerformActionUsage {}", pau.getQualifiedName());
					} else {
						if (pau.getType().size() > 1) {
							logger.error(
									"Too many Type associated to PerformActionUsage {}, only the first one will be used",
									pau.getQualifiedName());
						}
						switch (pau.getType().get(0)) {
						case ActionDefinition ad -> {
							MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(ad.getName())
									.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
									.addJavadoc("From $L\n$L", ad.getQualifiedName(), getDocString(ad.getDocumentation()));
							for (Feature param : ad.getParameter()) {
								Type attributeType = param.getType().getFirst();
								// possibly asks to generate the java class for the type
								addIndirectTypesToGenerate(attributeType);
								TypeName typeName = typeSwitch.doSwitch(attributeType);
								methodBuilder.addParameter(typeName, param.getName());
							}
							partDefInterfaceBuilder.addMethod(methodBuilder.build());
						}
						default -> throw new IllegalArgumentException("Unexpected value: " + pau.getType().get(0));
						}
					}
				} else {
					logger.warn("ignored action {} {} in PartDefinition {}", action.getName(),
							action.eClass().getName(), object.getName());
				}
			}

			// store javafile created for this partdef
			JavaFile javaFile = JavaFile.builder(packagePrefix + "." + getParentJavaPackageQualifiedName(object),
					partDefInterfaceBuilder.build()).build();
			transformationContext.classifierToJavaFile.put(object, javaFile);

			result.addAll(doSwitchForAllOwnedElements(object)); // look into children

			return result;
		}

		@Override
		public List<String> caseEnumerationDefinition(EnumerationDefinition object) {
			List<String> result = new ArrayList<>();
			logger.debug("traversing EnumerationDefinition {}", object.getName());
			TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(object.getName())
					.addModifiers(Modifier.PUBLIC);
			for (EnumerationUsage ev : object.getEnumeratedValue()) {
				enumBuilder.addEnumConstant(ev.getName());
			}
			JavaFile javaFile = JavaFile
					.builder(packagePrefix + "." + getParentJavaPackageQualifiedName(object), enumBuilder.build())
					.build();
			transformationContext.classifierToJavaFile.put(object, javaFile);
			return result;
		}

		/* Element is a top level inheritance */
		@Override
		public List<String> caseElement(Element object) {
			List<String> result = new ArrayList<>();
			logger.debug("traversing Element {} {}", object.getName(), object.getClass().getSimpleName());
			result.addAll(doSwitchForAllOwnedElements(object)); // look into owned children
			return result;
		}

		public List<String> doSwitchForAllOwnedElements(Element object) {
			List<String> result = new ArrayList<>();
			/* look into owned children */
			for (Element ownedElement : object.getOwnedElement()) {
				result.addAll(traversalSwitch.doSwitch(ownedElement));
			}
			return result;
		}
	}

	public List<String> generate(EObject rootSource) {
		List<String> result = traversalSwitch.doSwitch(rootSource);

		generateIndirectTypes();

		return result;
	}

	public String getDocString(EList<Documentation> docs) {
		return docs.stream().map(d -> d.getBody()).collect(Collectors.joining("\n"));
	}
	
	protected List<String> generateIndirectTypes() {
		List<String> result = new ArrayList<String>();
		while (!this.getContext().indirectTypesToGenerate.isEmpty()) {
			Type type = this.getContext().indirectTypesToGenerate.removeFirst();
			logger.info("Processing indirect Type {}", type.getQualifiedName());
			result.addAll(traversalSwitch.doSwitch(type));
		}
		return result;
	}

	/**
	 * add the given type to generate only if relevant and not already generated
	 * 
	 * @param type
	 */
	protected void addIndirectTypesToGenerate(Type type) {

		if (!this.getContext().indirectTypesToGenerate.contains(type)
				&& !this.getContext().classifierToJavaFile.containsKey(type)) {
			logger.debug("add Type {} to the list of Types be processed", type.getQualifiedName());
			this.getContext().indirectTypesToGenerate.add(type);
		} else {
			logger.debug("Type {} already available or planned to be processed", type.getQualifiedName());
		}
	}
}
