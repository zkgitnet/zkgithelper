package zkgithelper.files;

import zkgithelper.support.IoUtils;
import zkgithelper.support.AppConfig;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class FileUtils {

    public static final FileUtils INSTANCE = new FileUtils();

    private FileUtils() { }

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

    public static void zipDirectory(final String sourceDirPath,
                                    final String zipFileName) {
        Path sourceDir = Paths.get(sourceDirPath).toAbsolutePath();
        String tempDir = System.getProperty(AppConfig.JAVA_TMP);
        Path zipFilePath = Paths.get(tempDir,
                                     zipFileName
                                     + AppConfig.ZIP_SUFFIX).toAbsolutePath();

        try (ZipOutputStream zipOutputStream =
             new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            IoUtils.INSTANCE.trace(AppConfig.STATUS_COMPRESSING_START);
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(final Path file,
                                                     final BasicFileAttributes attrs)
                        throws IOException {
                        Path targetFile = sourceDir.relativize(file);
                        zipOutputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                        Files.copy(file, zipOutputStream);
                        zipOutputStream.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(final Path dir,
                                                             final BasicFileAttributes attrs)
                        throws IOException {
                        Path targetDir = sourceDir.relativize(dir);
                        if (!targetDir.toString().isEmpty()) {
                            zipOutputStream.putNextEntry(new ZipEntry(targetDir.toString()
                                                                      + AppConfig.SLASH_SEPARATOR));
                            zipOutputStream.closeEntry();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            IoUtils.INSTANCE.trace(AppConfig.STATUS_COMPRESSING_FINISH);
        } catch (IOException e) {
            IoUtils.INSTANCE.fatal(AppConfig.ERROR_COMPRESSING_FAILED
                                   + e.getMessage());
        }
    }

    public static void unzipDirectory(final String zipFileName,
                                      final String destDirName) {
        String tmpDir = System.getProperty(AppConfig.JAVA_TMP);
        Path zipFilePath = Paths.get(tmpDir, zipFileName);
        Path destDir = Paths.get(destDirName);

        if (Files.exists(zipFilePath)) {
            try (ZipInputStream zipInputStream =
                 new ZipInputStream(new FileInputStream(zipFilePath.toString()))) {
                IoUtils.INSTANCE.trace(AppConfig.STATUS_DECOMPRESSING_START);
                ZipEntry entry = zipInputStream.getNextEntry();
                while (entry != null) {
                    Path filePath = destDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        try (BufferedOutputStream bos =
                             new BufferedOutputStream(Files.newOutputStream(filePath))) {
                            byte[] buffer = new byte[AppConfig.ONE_KB];
                            int read;
                            while ((read = zipInputStream.read(buffer)) != -1) {
                                bos.write(buffer, 0, read);
                            }
                        }
                    }
                    zipInputStream.closeEntry();
                    entry = zipInputStream.getNextEntry();
                }
                Files.delete(zipFilePath);
                IoUtils.INSTANCE.trace(AppConfig.STATUS_DECOMPRESSING_FINISH);
            } catch (IOException e) {
                IoUtils.INSTANCE.fatal(AppConfig.ERROR_DECOMPRESSING_FAILED
                                       + e.getMessage());
            }
        }
    }
}
