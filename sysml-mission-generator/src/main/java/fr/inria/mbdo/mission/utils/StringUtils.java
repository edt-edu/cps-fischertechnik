package fr.inria.mbdo.mission.utils;

public class StringUtils {

	public static String toUpperFirst(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}
}
