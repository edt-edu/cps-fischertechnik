package fr.inria.mbdo.mission.utils;

import org.eclipse.syson.sysml.Element;

public class SymlUtils {

	public static String getParentQualifiedName(Element element) {		
		return element.getOwner().getQualifiedName();
	}
	
	public static String getParentJavaPackageQualifiedName(Element element) {		
		return element.getOwner().getQualifiedName().replaceAll("::", ".").toLowerCase();
	}
	
}
