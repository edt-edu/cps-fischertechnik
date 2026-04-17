package fr.inria.mbdo.mission.generators;


import static fr.inria.mbdo.mission.utils.SymlUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.syson.sysml.AttributeUsage;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Namespace;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;


public class MachineInterfaceGenerator {
	private static final Logger logger = LoggerFactory.getLogger(MachineInterfaceGenerator.class);

	String packagePrefix;
	MachineInterfaceGeneratorSwitch traversalSwitch;
	SymlToJavaTypeSwitch typeSwitch;

	public MachineInterfaceGenerator(String packagePrefix) {
		TransformationContext context = new TransformationContext();
		traversalSwitch = new MachineInterfaceGeneratorSwitch(context);
		typeSwitch = new SymlToJavaTypeSwitch(packagePrefix);
		this.packagePrefix = packagePrefix;
	}

	/**
	 * class used to store the generator context ie. element to be printed in the
	 * end note: depending on the transformation/code gen it can be replaced by the
	 * return type of the do switch
	 */
	public class TransformationContext {

		/**
		 * map containing the sysml element as key and the created SysmlRTD object as
		 * value used to ease the creation of the SysmlRTD object when navigating the
		 * sysml model
		 */
		// public Map<Element, UsageValue> sysmlToRTDContextMap = new HashMap<>();

		// sysml source root
		// sysmlrtd target root (returned as result
		
		public Map<PartDefinition, TypeSpec.Builder> partDefToJavaInterface = new HashMap<>(); 
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

			TypeSpec.Builder partDefInterfaceBuilder = TypeSpec.interfaceBuilder(object.getName());
			partDefInterfaceBuilder.addModifiers(Modifier.PUBLIC);
			
			for(AttributeUsage ownedAttribute : object.getOwnedAttribute()) {
				
				// attribute type
				
				MethodSpec.Builder getterBuilder = MethodSpec.methodBuilder("get"+ownedAttribute.getName()).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
				logger.debug("computing Java type for attribute {}", ownedAttribute.getQualifiedName());
				if(ownedAttribute.getType().isEmpty()) {
					logger.error("Missing attribute type for {}", ownedAttribute.getQualifiedName());
				}
				if(ownedAttribute.getType().size() > 1) {
					logger.error("Multiple type attribute not supported for {}", ownedAttribute.getQualifiedName());
				}
				getterBuilder.returns(typeSwitch.doSwitch(ownedAttribute.getType().getFirst()));
				
				MethodSpec.Builder setterBuilder = MethodSpec.methodBuilder("set"+ownedAttribute.getName()).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
				
				partDefInterfaceBuilder.addMethod(setterBuilder.build());
				partDefInterfaceBuilder.addMethod(getterBuilder.build());
			}
			
			transformationContext.partDefToJavaInterface.put(object, partDefInterfaceBuilder);
			
			result.addAll(doSwitchForAllOwnedElements(object)); // look into children

			return result;
		}

		/* Element is a top level inheritance */
        @Override
        public List<String> caseElement(Element object) {
            List<String> result = new ArrayList<>();
            logger.debug("traversing Element {} {}", object.getName(), object.getClass().getSimpleName() );
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
		
		
		
		return traversalSwitch.doSwitch(rootSource);
	}

}
