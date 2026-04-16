package fr.inria.mbdo.mission;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;

public class TestUtils {

    public static void assertNodeAvailable() {
        try {
            Process process = new ProcessBuilder("node", "-v")
                    .redirectErrorStream(true)
                    .start();

            int exitCode = process.waitFor();

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
