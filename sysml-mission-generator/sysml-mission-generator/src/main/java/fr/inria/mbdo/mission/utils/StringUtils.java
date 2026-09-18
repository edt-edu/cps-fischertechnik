package fr.inria.mbdo.mission.utils;

import org.eclipse.syson.sysml.Element;

public class StringUtils {

	public static String toUpperFirst(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	public static String getParentJavaPackageQualifiedName(Element element) {
		return element.getOwner().getQualifiedName().replaceAll("::", ".").toLowerCase();
	}
}
