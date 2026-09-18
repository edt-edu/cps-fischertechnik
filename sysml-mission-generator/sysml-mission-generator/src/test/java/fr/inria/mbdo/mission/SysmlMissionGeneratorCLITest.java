package fr.inria.mbdo.mission;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

/**
 * Unit test for simple App.
 */
public class SysmlMissionGeneratorCLITest {

    @Test
    void shouldPrintHelp() {
        // given
        SysmlMissionGeneratorCLI cli = new SysmlMissionGeneratorCLI();
        CommandLine cmd = new CommandLine(cli);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(out, true));

        // when
        int exitCode = cmd.execute("--help");

        // then
        assertEquals(0, exitCode);
        String output = out.toString();

        assertTrue(output.contains("Usage:"), "Should contain usage section");
        assertTrue(output.contains("SysmlMissionGenerator"), "Should contain command name");
        assertTrue(output.contains("--input"), "Should document input option");
        assertTrue(output.contains("--output"), "Should document output option");
    }
}
