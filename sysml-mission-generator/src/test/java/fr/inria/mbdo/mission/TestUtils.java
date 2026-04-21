package fr.inria.mbdo.mission;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import fr.inria.mbdo.mission.importer.SysmlImporter;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

	public static void assertNodeAvailable() {
		LOGGER.info("system PATH=" + System.getenv("PATH"));
		try {
			Process process = new ProcessBuilder("node", "-v").redirectErrorStream(true).start();

			int exitCode = process.waitFor();
			if(exitCode != 0) LOGGER.error("Node.js is not available in PATH or returned non-zero exit code");
			Assertions.assertEquals(0, exitCode, "Node.js is not available in PATH or returned non-zero exit code");

		} catch (IOException e) {
			LOGGER.error("Node.js executable not found in PATH");
			Assertions.fail("Node.js executable not found in PATH", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("Process interrupted while checking Node.js");
			Assertions.fail("Process interrupted while checking Node.js", e);
		}
	}
}
