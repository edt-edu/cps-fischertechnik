package fr.inria.mbdo.mission.generators;


import static fr.inria.mbdo.mission.utils.SymlUtils.*;

import org.eclipse.syson.sysml.DataType;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.EnumerationDefinition;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;

/**
 * switch that returns the equivalent java class as a JavaPoet TypeName for the
 * given SysML type
 */
public class SymlToJavaTypeSwitch extends SysmlSwitch<TypeName> {
	private static final Logger logger = LoggerFactory.getLogger(SymlToJavaTypeSwitch.class);

	String packagePrefix;

	public SymlToJavaTypeSwitch(String packagePrefix) {
		this.packagePrefix = packagePrefix;
	}

	@Override
	public TypeName caseDataType(DataType object) {
		String qualifiedName = object.getQualifiedName();
		switch (qualifiedName) {
		case "ScalarValues::Boolean": {
			return TypeName.get(boolean.class);
		}
		case "ScalarValues::Integer": {
			return TypeName.get(int.class);
		}
		default:
			String msg = "Type mapping not implemented for: " + object.eClass().getName() + ". An example is "+object.getQualifiedName();
			throw new UnsupportedOperationException(msg);
		}
		
	}
	
	@Override
	public TypeName caseEnumerationDefinition(EnumerationDefinition object) {
		TypeName type = ClassName.get(packagePrefix+"."+getParentJavaPackageQualifiedName(object), object.getName());
		return type;
	}

	@Override
	public TypeName caseElement(Element object) {
		String msg = "Type mapping not implemented for: " + object.eClass().getName() + ". An example is "+object.getQualifiedName();
		logger.error(msg);
		throw new UnsupportedOperationException(msg);
	}

}
