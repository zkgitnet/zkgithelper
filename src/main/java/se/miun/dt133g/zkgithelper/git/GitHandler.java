package se.miun.dt133g.zkgithelper.git;

import se.miun.dt133g.zkgithelper.connection.GitConnection;
import se.miun.dt133g.zkgithelper.support.AppConfig;
import se.miun.dt133g.zkgithelper.support.IoUtils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Singleton class responsible for managing Git operations such as push, fetch, and list.
 * Uses JGit for low-level Git manipulation and supports temporary and main repositories.
 * @author Leif Rogell
 */
public final class GitHandler {

    public static final GitHandler INSTANCE = new GitHandler();

    private Git git;
    private Git tmpGit;
    private Repository repository;
    private Repository tmpRepository;
    private String repoPath;
    private String tmpRepoPath;
    private int processes = AppConfig.MAX_NUM_PROCESSES;
    private ConcurrentMap<String, Ref> refs = new ConcurrentHashMap<>();
    private Map<String, String> pushed = new HashMap<>();
    private boolean firstPush = false;
    private String repoName;

    private GitHandler() { }

    /**
     * Sets the repository name and logs it for debugging.
     * @param repoName the name of the repository
     */
    public void setRepoName(final String repoName) {
        this.repoName = repoName;
    }

    /**
     * Initializes the temporary Git repository from the given path.
     * @param path the path to the temporary repository's .git directory
     */
    public void setTmpRepoPath(final String path) {
        this.tmpRepoPath = path;
        try {
            this.tmpRepository = new FileRepositoryBuilder()
                .setGitDir(new File(path))
                .readEnvironment()
                .findGitDir()
                .build();
            this.tmpGit = new Git(tmpRepository);

        } catch (IOException e) {
            IoUtils.INSTANCE.fatal("Failed to set up repository: "
                                   + e.getMessage());
        }
    }

    /**
     * Initializes the main Git repository from the given path.
     * @param path the path to the main repository
     */
    public void setRepoPath(final String path) {
        this.repoPath = path;
        try {
            this.repository = new FileRepositoryBuilder()
                .setGitDir(new File(path + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build();
            this.git = new Git(repository);

        } catch (IOException e) {
            IoUtils.INSTANCE.fatal("Failed to set up repository: "
                                   + e.getMessage());
        }
    }

    /**
     * Extracts the name of a repository from its URL.
     * @param url the URL of the repository
     * @return the repository name without the ".git" suffix
     */
    public String extractRepoName(final String url) {
        int lastSlashIndex =
            url.lastIndexOf(AppConfig.SLASH_SEPARATOR);
        String repoNameWithSuffix =
            url.substring(lastSlashIndex + 1);
        int lastDotIndex =
            repoNameWithSuffix.lastIndexOf(AppConfig.DOT_SEPARATOR);

        return lastDotIndex != -1
            ? repoNameWithSuffix.substring(0, lastDotIndex)
            : repoNameWithSuffix;
    }

    /**
     * Lists Git references in the repository and prints their object IDs and names.
     * Handles both pull and push cases and reports the HEAD ref if applicable.
     * @param line the command input line
     */
    public void doList(final String line) {
        boolean forPush = line.contains(AppConfig.GIT_FOR_PUSH);

        try {
            if (GitConnection.INSTANCE.requestFile(repoPath,
                                                   calculateRepoSignature(!forPush))
                .contains(AppConfig.STATUS_REPO_UPTODATE)) {
                IoUtils.INSTANCE.fatal(AppConfig.GIT_END);
                return;
            }

            List<Ref> refs = getRefs();

            for (Ref ref : refs) {
                String refName = ref.getName();
                String refObjectId = ref.getObjectId().getName();
                IoUtils.INSTANCE.write(refObjectId + AppConfig.SPACE_SEPARATOR + refName);
            }

            if (!forPush) {
                Ref head = repository.exactRef(AppConfig.GIT_HEAD);
                if (head != null) {
                    String headTargetName = head.getTarget().getName();
                    IoUtils.INSTANCE.write(AppConfig.AT_SEPARATOR + headTargetName
                                           + AppConfig.SPACE_SEPARATOR + AppConfig.GIT_HEAD);
                } else {
                    IoUtils.INSTANCE.trace(AppConfig.STATUS_NO_DEFAULT_BRANCH);
                }
            }

            IoUtils.INSTANCE.write(AppConfig.GIT_END);
        } catch (IOException e) {
            IoUtils.INSTANCE.fatal("git list failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Pushes changes to a remote repository. Handles first-time push,
     * reference updates, and sending objects to the server.
     * @param line the command input line containing source and destination references
     */
    public void doPush(final String line) {
        String[] parts = line.split(AppConfig.SPACE_SEPARATOR);
        String src = parts[1].split(AppConfig.COLON_SEPARATOR)[0].replaceFirst("^\\+", "");
        String dst = parts[1].split(AppConfig.COLON_SEPARATOR)[1];

        try {
            if (src.equals(AppConfig.GIT_END)) {
                delete(dst);
            } else {
                copyAllObjects(Paths.get(repository.getDirectory().toString(), AppConfig.GIT_OBJECTS),
                               Paths.get(tmpRepoPath, AppConfig.GIT_OBJECTS));
                push(src, dst);
                if (firstPush) {
                    Ref remoteHead = repository.exactRef(AppConfig.GIT_HEAD);
                    if (remoteHead != null) {
                        if (!writeSymbolicRef(AppConfig.GIT_HEAD, dst)) {
                            IoUtils.INSTANCE.trace(AppConfig.ERROR_REMOTE_BRANCH_FAIL);
                        }
                    } else {
                        IoUtils.INSTANCE.trace(AppConfig.ERROR_FIRST_REMOTE_BRANCH_FAIL);
                    }
                }
                String response = GitConnection.INSTANCE.sendFile(repoPath,
                                                                  calculateRepoSignature(true));

                if (response == null
                    || !response.contains(AppConfig.COMMAND_SUCCESS)) {
                    IoUtils.INSTANCE.fatal(AppConfig.ERROR_REPO_TRANSFER_FAILED
                                           + line);
                } else {
                    IoUtils.INSTANCE.trace(AppConfig.STATUS_SEND_FINISH);
                }
            }
            IoUtils.INSTANCE.write(AppConfig.GIT_END);
        } catch (IOException e) {
            IoUtils.INSTANCE.fatal("push failed");
        }
    }

    /**
     * Copies all object files from the source .git/objects directory to the target location.
     * @param sourceObjectsPath the source path of Git objects
     * @param targetObjectsPath the target path for copied Git objects
     * @throws IOException if a file operation fails
     */
    private void copyAllObjects(final Path sourceObjectsPath,
                                final Path targetObjectsPath) throws IOException {
        Files.walk(sourceObjectsPath)
            .filter(Files::isRegularFile)
            .forEach(sourcePath -> {
                    try {
                        Path relativePath = sourceObjectsPath.relativize(sourcePath);
                        Path targetPath = targetObjectsPath.resolve(relativePath);

                        if (Files.exists(targetPath)) {
                            return;
                        }

                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        IoUtils.INSTANCE.fatal("Failed to copy object: "
                                               + sourcePath.toString() + " - " + e.getMessage());
                    }
                });
    }

    /**
     * Fetches an object from the temporary repository into the main repository.
     * @param line the command input line containing object SHA and reference name
     */
    public void doFetch(final String line) {
        String[] parts = line.split(AppConfig.SPACE_SEPARATOR);
        String sha = parts[1];
        String ref = parts[2];
        try {
            copyAllObjects(Paths.get(tmpRepoPath, AppConfig.GIT_OBJECTS),
                           Paths.get(repository.getDirectory().toString(), AppConfig.GIT_OBJECTS));
            //fetch(sha, ref);
        } catch (IOException e) {
            e.printStackTrace();
        }
        IoUtils.INSTANCE.write(AppConfig.GIT_END);
    }

    /**
     * Deletes a branch reference from the repository and updates internal state.
     * @param ref the name of the reference to delete
     */
    private void delete(final String ref) {
        try {
            git.branchDelete().setBranchNames(ref).setForce(true).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        refs.remove(ref);
        pushed.remove(ref);
        IoUtils.INSTANCE.write(AppConfig.GIT_OK + ref);
    }

    /**
     * Pushes the specified source reference to the destination reference.
     * Copies necessary Git objects and writes updated refs.
     * @param src the source reference (e.g., branch name or commit SHA)
     * @param dst the destination reference in the remote repo
     */
    private void push(String src, final String dst) {
        boolean force = false;
        if (src.startsWith(AppConfig.GIT_FORCE)) {
            src = src.substring(1);
            force = true;
        }

        List<String> present = new ArrayList<>(refs.keySet());
        present.addAll(pushed.values());

        List<ObjectId> objects;
        try {
            objects = getGitObjects(repoPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(processes);

        for (ObjectId obj : objects) {
            pool.execute(() -> {
                    try {
                        putObject(obj);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        }

        pool.shutdown();

        String sha;
        try {
            sha = repository.resolve(src).getName();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String error = writeRef(sha, dst, force);

        if (error == null) {
            IoUtils.INSTANCE.write(AppConfig.GIT_OK + dst);
            pushed.put(dst, sha);
        } else {
            IoUtils.INSTANCE.write(AppConfig.GIT_ERROR + dst + AppConfig.SPACE_SEPARATOR + error);
        }
    }

    /**
     * Retrieves all Git object IDs from the specified repository's objects directory.
     * @param gitDir the path to the Git repository
     * @return a list of Git object IDs
     * @throws IOException if the objects directory is invalid or unreadable
     */
    private List<ObjectId> getGitObjects(final String gitDir) throws IOException {
        List<ObjectId> objects = new ArrayList<>();
        File objectsDir = new File(gitDir + "/.git/", "objects");

        if (!objectsDir.exists() || !objectsDir.isDirectory()) {
            throw new IOException("Not a valid Git objects directory: " + objectsDir.getAbsolutePath());
        }

        Files.walk(Paths.get(objectsDir.getAbsolutePath()))
             .filter(Files::isRegularFile)
             .map(path -> path.getParent().getFileName() + path.getFileName().toString())
             .map(hash -> {
                 try {
                     return ObjectId.fromString(hash);
                 } catch (IllegalArgumentException e) {
                     return null;
                 }
             })
             .filter(obj -> obj != null)
             .collect(Collectors.toCollection(() -> objects));

        return objects;
    }

    /**
     * Copies a single Git object file from the main repository to the temporary repository.
     *
     * @param sha the object ID to copy
     * @throws IOException if the copy operation fails
     */
    private void putObject(final ObjectId sha) throws IOException {
        Path sourcePath = Paths.get(repository.getDirectory().toString(),
                                    AppConfig.GIT_OBJECTS,
                                    sha.name().substring(0, 2),
                                    sha.name().substring(2));

        Path targetPath = Paths.get(tmpRepoPath,
                                    AppConfig.GIT_OBJECTS,
                                    sha.name().substring(0, 2),
                                    sha.name().substring(2));

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            IoUtils.INSTANCE.trace("putObject ERROR: " + e.getMessage());
        }
    }

    /**
     * Retrieves all branch references from the temporary Git repository.
     * @return a list of branch references, or an empty list if retrieval fails
     */
    private List<Ref> getRefs() {
        try {
            return tmpGit.branchList().call();
        } catch (GitAPIException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Writes a new reference (branch or tag) in the temporary repository.
     * Supports fast-forward and force updates, and prevents non-fast-forwards.
     * @param sha the SHA of the commit to point to
     * @param ref the reference name (e.g., refs/heads/main)
     * @param force whether to force the update regardless of history
     * @return null if successful, or an error message string if failed
     */
    private String writeRef(final String sha, final String ref, final boolean force) {
        Ref oldRef = refs.get(ref);
        if (oldRef != null) {
            try {
                ObjectId newObjectId = ObjectId.fromString(sha);
                ObjectId oldObjectId = oldRef.getObjectId();

                if (!force && !isAncestor(oldObjectId, newObjectId, tmpRepository)) {
                    return AppConfig.ERROR_NON_FAST_FORWARD;
                }
                if (isAncestor(newObjectId, oldObjectId, tmpRepository)) {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return AppConfig.ERROR_CANNOT_WRITE_REF;
            }
        } else {

            try {
                RefUpdate refUpdate = tmpRepository.updateRef(ref);
                refUpdate.setNewObjectId(ObjectId.fromString(sha));
                refUpdate.setForceUpdate(force);
                RefUpdate.Result result = refUpdate.update();

                if (result == RefUpdate.Result.NEW || result == RefUpdate.Result.FORCED
                    || result == RefUpdate.Result.FAST_FORWARD) {
                    refs.put(ref, tmpRepository.findRef(ref));
                    return null;
                } else {
                    String errorMessage = "RefUpdate failed with result: " + result.toString();
                    IoUtils.INSTANCE.trace(errorMessage);
                    return AppConfig.ERROR_CANNOT_WRITE_REF + " - " + errorMessage;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return AppConfig.ERROR_CANNOT_WRITE_REF;
            }
        }
        return null;
    }

    /**
     * Writes a symbolic reference (e.g., HEAD - refs/heads/main) in the main repository.
     * @param symbolic the symbolic ref to update (e.g., HEAD)
     * @param ref the reference it should point to
     * @return true if update was successful, false otherwise
     */
    private boolean writeSymbolicRef(final String symbolic,
                                     final String ref) {
        try {

            RefUpdate refUpdate = repository.updateRef(symbolic);
            refUpdate.link(ref);
            RefUpdate.Result result = refUpdate.update();

            return result == RefUpdate.Result.NEW
                || result == RefUpdate.Result.FORCED;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Computes a SHA-256 signature of the current repository's commit history.
     * Used to detect if the repository state has changed.
     * @param includeCurrentCommit whether to include the current commit in the hash
     * @return a hex string of the calculated signature, or null if computation fails
     */
    private String calculateRepoSignature(final boolean includeCurrentCommit) {
        try (RevWalk revWalk = new RevWalk(repository)) {
            Iterable<RevCommit> commits = git.log().call();

            List<String> commitHashes = StreamSupport.stream(commits.spliterator(), false)
                //.skip(includeCurrentCommit ? 0 : 1)
                .map(RevCommit::getId)
                .map(ObjectId::getName)
                .sorted()
                .collect(Collectors.toList());

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            IoUtils.INSTANCE.trace("Size: " + commitHashes.size());
            for (String hash : commitHashes) {
                digest.update(hash.getBytes());
            }
            byte[] combinedHashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : combinedHashBytes) {
                sb.append(String.format("%02x", b));
            }
            IoUtils.INSTANCE.trace(sb.toString());
            return sb.toString();
        } catch (NoSuchAlgorithmException | GitAPIException e) {
            return null;
        }
    }

    /**
     * Checks whether a commit is an ancestor of another commit in the repository history.
     * @param ancestorId the possible ancestor commit ID
     * @param refId the reference commit ID
     * @param repository the Git repository to search
     * @return true if ancestorId is an ancestor of refId, false otherwise
     * @throws IOException if commit parsing fails
     */
    private boolean isAncestor(final ObjectId ancestorId,
                              final ObjectId refId,
                              final Repository repository)
        throws IOException {
        try (RevWalk walk =
             new RevWalk(repository)) {
            RevCommit ancestorCommit = walk.parseCommit(ancestorId);
            RevCommit refCommit = walk.parseCommit(refId);
            return walk.isMergedInto(ancestorCommit, refCommit);
        }
    }

    /*public void fetch(final String sha, final String refName) throws IOException {
    // Download the object specified by sha
    download(sha);

    // Update the reference in the new Git directory
    Path refPath = Paths.get(repoPath, refName);
    Files.createDirectories(refPath.getParent());

    // Write the SHA to the reference file
    Files.write(refPath, sha.getBytes(StandardCharsets.UTF_8),
    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

    IoUtils.INSTANCE.trace("Fetched " + sha + " and updated reference " + refName);
    }

    private void download(final String sha) throws IOException {
    Path sourceObjectPath = Paths.get(tmpRepoPath, AppConfig.GIT_OBJECTS, sha.substring(0, 2), sha.substring(2));
    Path targetObjectPath = Paths.get(repoPath, AppConfig.GIT_OBJECTS, sha.substring(0, 2), sha.substring(2));

    if (Files.exists(sourceObjectPath)) {
    // Ensure the target directory exists
    Files.createDirectories(targetObjectPath.getParent());

    // Copy the object from the temporary repository to the new one
    Files.copy(sourceObjectPath, targetObjectPath, StandardCopyOption.REPLACE_EXISTING);

    // Verify the integrity of the copied object
    byte[] data = Files.readAllBytes(targetObjectPath);
    ObjectId computedSha = ObjectId.fromRaw(data);
    if (!computedSha.name().equals(sha)) {
    throw new RuntimeException(AppConfig.ERROR_HASH_MISMATCH + computedSha + " != " + sha);
    }

    IoUtils.INSTANCE.trace("Downloaded and verified object " + sha);
    } else {
    throw new IOException("Object not found: " + sha);
    }
    }*/
}
