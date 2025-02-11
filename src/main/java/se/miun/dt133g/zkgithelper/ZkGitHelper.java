package se.miun.dt133g.zkgithelper;

import zkgithelper.git.GitHandler;
import zkgithelper.files.FileUtils;
import zkgithelper.connection.GitConnection;
import zkgithelper.support.AppConfig;
import zkgithelper.support.IoUtils;

import java.util.Scanner;

public final class ZkGitHelper {

    private ZkGitHelper() { }

    public static void main(final String[] args) {
        Scanner scanner = IoUtils.INSTANCE.getScanner();

        /*for (String arg : args) {
            IoUtils.INSTANCE.trace("arg: " + arg);
            }*/

        GitConnection.INSTANCE.setDstPort(args[1]);
        GitConnection.INSTANCE.ensureConnected();

        String repoName = GitHandler.INSTANCE.extractRepoName(args[1]);
        String dirName = GitHandler.INSTANCE.extractRepoName(args[2]);
        String repoPath = repoName.equals(dirName)
            ? args[2] : args[2] + "/" + repoName;
        
        GitHandler.INSTANCE.setRepoName(repoName);
        GitHandler.INSTANCE.setRepoPath(repoPath);
        GitHandler.INSTANCE.setTmpRepoPath(FileUtils.INSTANCE.createTmpDirectory(repoName));
        GitConnection.INSTANCE.cleanTmp(repoName);

        while (true) {
            String line = scanner.nextLine();
            //IoUtils.INSTANCE.trace("ZkGitHelper line: " + line);

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
        GitConnection.INSTANCE.cleanTmp(repoName);
        IoUtils.INSTANCE.trace(AppConfig.STATUS_ZKGIT_FINISH);
    }
}
