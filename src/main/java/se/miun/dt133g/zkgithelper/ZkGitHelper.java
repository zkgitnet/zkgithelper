package se.miun.dt133g.zkgithelper;

import se.miun.dt133g.zkgithelper.git.GitHandler;
import se.miun.dt133g.zkgithelper.files.FileUtils;
import se.miun.dt133g.zkgithelper.connection.GitConnection;
import se.miun.dt133g.zkgithelper.support.AppConfig;
import se.miun.dt133g.zkgithelper.support.IoUtils;

import java.util.Scanner;


/**
 * Entry point for the ZkGitHelper application, responsible for managing a Git-like
 * remote interaction over a custom protocol. It initializes the Git connection,
 * prepares the repository environment, and processes input commands.
 * @author Leif Rogell
 */
public final class ZkGitHelper {

    private ZkGitHelper() { }

    /**
     * Launches the application, sets up remote connection parameters and repository info,
     * then enters a loop to handle incoming Git-like commands (push, fetch, list, etc.).
     * @param args Command-line arguments: [0] is unused, [1] is destination port, [2] is target repository directory
     */
    public static void main(final String[] args) {
        Scanner scanner = IoUtils.INSTANCE.getScanner();

        GitConnection.INSTANCE.setDstPort(args[1]);
        GitConnection.INSTANCE.ensureConnected();

        String repoName = GitHandler.INSTANCE.extractRepoName(args[1]);
        String dirName = GitHandler.INSTANCE.extractRepoName(args[2]);
        String repoPath = repoName.equals(dirName) ? args[2] : args[2] + "/" + repoName;

        GitHandler.INSTANCE.setRepoName(repoName);
        GitHandler.INSTANCE.setRepoPath(repoPath);
        //GitConnection.INSTANCE.cleanTmp(repoName);
        GitHandler.INSTANCE.setTmpRepoPath(FileUtils.INSTANCE.createTmpDirectory(repoName));

        while (true) {
            String line = scanner.nextLine();

            if (line.equals(AppConfig.GIT_CAPABILITIES)) {
                IoUtils.INSTANCE.write(AppConfig.GIT_PUSH);
                IoUtils.INSTANCE.write(AppConfig.GIT_FETCH);
                IoUtils.INSTANCE.write(AppConfig.GIT_END);
            } else if (line.startsWith(AppConfig.GIT_LIST)) {
                GitHandler.INSTANCE.doList(line);
            } else if (line.startsWith(AppConfig.GIT_PUSH)) {
                IoUtils.INSTANCE.trace(AppConfig.STATUS_BEGIN_PUSH);
                GitHandler.INSTANCE.doPush(line);
            } else if (line.startsWith(AppConfig.GIT_FETCH)) {
                GitHandler.INSTANCE.doFetch(line);
            } else if (line.equals(AppConfig.GIT_END)) {
                break;
            } else {
                IoUtils.INSTANCE.fatal(AppConfig.ERROR_UNSUPPORTED_OPERATION
                                       + line);
            }
        }
        //GitConnection.INSTANCE.cleanTmp(repoName);
        IoUtils.INSTANCE.trace(AppConfig.STATUS_ZKGIT_FINISH);
    }
}
