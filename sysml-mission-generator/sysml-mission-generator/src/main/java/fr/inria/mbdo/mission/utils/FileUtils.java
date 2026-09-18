package fr.inria.mbdo.mission.utils;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

	/**
	 * get Path for resources in /src/main/resources or src/test/resources
	 * report with a better exception message instead of null pointer in case of missing resource
	 * @param resourcePath
	 * @return
	 */
	public static Path getResourcePath(String resourcePath) {
	    URL url = FileUtils.class.getClassLoader().getResource(resourcePath);

	    if (url == null) {
	        throw new IllegalArgumentException(
	            "Resource not found on classpath: " + resourcePath
	        );
	    }

	    try {
	        return Paths.get(url.toURI());
	    } catch (URISyntaxException e) {
	        throw new RuntimeException(
	            "Invalid URI for resource: " + resourcePath, e
	        );
	    }
	}
}
