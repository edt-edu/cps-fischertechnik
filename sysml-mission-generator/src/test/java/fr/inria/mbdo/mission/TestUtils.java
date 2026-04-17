package fr.inria.mbdo.mission;

import java.io.IOException;

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
			LOGGER.error("Node.js is not available in PATH or returned non-zero exit code");
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
