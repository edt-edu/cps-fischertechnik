package fr.inria.mbdo.mission.utils;

import java.util.Locale;

public final class SysmlToJavaUtils {

    private SysmlToJavaUtils() {
    }

    public static String javaPackage(String packagePrefix, String namespace) {
        StringBuilder packageName = new StringBuilder(packagePrefix);

        if (namespace != null) {
            packageName.append('.').append(namespace.replace("::", ".").toLowerCase(Locale.ROOT));
        }

        return packageName.toString();
    }

    public static String javaClass(String name) {
        return name.replace("_", "");
    }
}
