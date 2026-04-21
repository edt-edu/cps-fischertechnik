package fr.inria.mbdo.mission.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImportUtils {
    /**
     * This method applies transformations to file content before importing the file to sysml importer.
     * The file content is not written on disk as it uses a temporary file to do the math without affecting existing content.
     * @param inputFile File to apply transformation on its content.
     * @return File containing the transformed content.
     * @throws IOException
     */
    public static File applyFileTransformations(File inputFile) throws IOException {
        String content = Files.readString(inputFile.toPath());

        // Rule definitions here:
        content = content.replace(" send new ", " send ");

        Path temp = Files.createTempFile("modified-", ".sysml");
        Files.writeString(temp, content);
        File file = temp.toFile();
        file.deleteOnExit();

        return file;
    }
}
