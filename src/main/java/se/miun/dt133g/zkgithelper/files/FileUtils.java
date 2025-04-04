package se.miun.dt133g.zkgithelper.files;

import se.miun.dt133g.zkgithelper.support.IoUtils;
import se.miun.dt133g.zkgithelper.support.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for handling file system operations such as creating temporary directories.
 * @author Leif Rogell
 */
public final class FileUtils {

    public static final FileUtils INSTANCE = new FileUtils();

    private FileUtils() { }

    /**
     * Creates a temporary directory for the given repository name in the system temp path.
     * @param repoName the name of the repository
     * @return the full path to the created directory, or null if creation fails
     */
    public String createTmpDirectory(final String repoName) {
        try {

            Path tmpDir = Paths.get(System.getProperty(AppConfig.JAVA_TMP),
                                    AppConfig.TMP_PREFIX
                                    + repoName);

            if (!Files.exists(tmpDir)) {
                Files.createDirectory(tmpDir);
            }
            return tmpDir.toString();
        } catch (IOException e) {
            IoUtils.INSTANCE.fatal(AppConfig.ERROR_CREATE_TMP_DIRECTORY
                                   + e.getMessage()
                                   + AppConfig.NEW_LINE);
            return null;
        }
    }
}
