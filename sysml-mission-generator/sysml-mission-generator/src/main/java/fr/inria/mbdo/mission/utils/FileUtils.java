package fr.inria.mbdo.mission.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	/**
     * Recursively searches {@code startPath} for files whose name ends with the given {@code extension}.
     * <p>
     * Example usage :
     * <pre>
     * Path projectPath = Paths.get(System.getProperty("user.dir"))
     *                         .resolve("../missions-design-models")
     *                         .normalize();
     * List&lt;Path&gt; sysmlFiles = FileUtils.findFilesByExtension(projectPath, ".sysml");
     * </pre>
     *
     * @param startPath the directory from which the search begins; must exist and be a directory
     * @param extension the file extension to match, including the leading dot (e.g. {@code ".sysml"})
     * @return a list of {@link Path}s to files that match the extension; the list is empty if none are found
     * @throws IOException if an I/O error occurs while walking the file tree
     */
    public static List<Path> findFilesByExtension(Path startPath, String extension) throws IOException {
        if (startPath == null || !Files.isDirectory(startPath)) {
            throw new IllegalArgumentException("startPath must be an existing directory: " + startPath);
        }
        try (Stream<Path> stream = Files.walk(startPath)) {
            return stream.filter(Files::isRegularFile)
                         .filter(p -> p.getFileName().toString().endsWith(extension))
                         .collect(Collectors.toList());
        }
    }
}
