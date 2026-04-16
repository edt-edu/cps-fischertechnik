package fr.inria.mbdo.mission;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TestUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);
    public static void assertNodeAvailable() {
        try {
            Process process = new ProcessBuilder("node", "-v")
                    .redirectErrorStream(true)
                    .start();

            int exitCode = process.waitFor();


            LOGGER.info("system PATH=" + System.getenv("PATH"));
            Assertions.assertEquals(
                0,
                exitCode,
                "Node.js is not available in PATH or returned non-zero exit code"
            );

        } catch (IOException e) {
            Assertions.fail("Node.js executable not found in PATH", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assertions.fail("Process interrupted while checking Node.js", e);
        }
    }

}
