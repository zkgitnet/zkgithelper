package se.miun.dt133g.zkgithelper.git;

import se.miun.dt133g.zkgithelper.connection.GitConnection;
import se.miun.dt133g.zkgithelper.files.FileUtils;
import se.miun.dt133g.zkgithelper.support.AppConfig;
import se.miun.dt133g.zkgithelper.support.IoUtils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.errors.MissingObjectException;
import java.net.URISyntaxException;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.zip.InflaterInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    public void setRepoName(final String repoName) {
        this.repoName = repoName;
    }

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

    public void doList(final String line) {
        boolean forPush = line.contains(AppConfig.GIT_FOR_PUSH);

        try {
            GitConnection.INSTANCE.requestFile(repoName + AppConfig.ZIP_SUFFIX,
                                               calculateRepoSignature(!forPush));
            FileUtils.INSTANCE.unzipDirectory(repoName + AppConfig.ZIP_SUFFIX, tmpRepoPath);
        
            List<Ref> refs = getRefs();
            //IoUtils.INSTANCE.trace("Number of refs fetched: " + refs.size());

            for (Ref ref : refs) {
                String refName = ref.getName();
                String refObjectId = ref.getObjectId().getName();
                //IoUtils.INSTANCE.trace("Processing ref - Name: " + refName + ", ObjectId: " + refObjectId);
                IoUtils.INSTANCE.write(refObjectId + AppConfig.SPACE_SEPARATOR + refName);
            }

            if (!forPush) {
                Ref head = repository.exactRef(AppConfig.GIT_HEAD);
                if (head != null) {
                    String headTargetName = head.getTarget().getName();
                    //IoUtils.INSTANCE.trace("HEAD Ref - Target Name: " + headTargetName);
                    IoUtils.INSTANCE.write(AppConfig.AT_SEPARATOR + headTargetName + AppConfig.SPACE_SEPARATOR + AppConfig.GIT_HEAD);
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


    public void doPush(final String line) {
        String[] parts =
            line.split(AppConfig.SPACE_SEPARATOR);
        String src =
            parts[1].split(AppConfig.COLON_SEPARATOR)[0].replaceFirst("^\\+",
                                                                      "");
        String dst =
            parts[1].split(AppConfig.COLON_SEPARATOR)[1];

        try {
            if (src.equals(AppConfig.GIT_END)) {
                delete(dst);
            } else {
                push(src, dst);
                copyAllObjects(Paths.get(repository.getDirectory().toString(), AppConfig.GIT_OBJECTS),
                               Paths.get(tmpRepoPath, AppConfig.GIT_OBJECTS));
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
                FileUtils.INSTANCE.zipDirectory(tmpRepoPath,
                                                repoName);
                String response = GitConnection.INSTANCE.sendFile(repoName
                                                                  + AppConfig.ZIP_SUFFIX,
                                                                  calculateRepoSignature(true));

                if (response == null
                    || !response.equals(AppConfig.COMMAND_SUCCESS)) {
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

    private void copyAllObjects(Path sourceObjectsPath, Path targetObjectsPath) throws IOException {
    // Traverse the source .git/objects directory
    Files.walk(sourceObjectsPath)
        .filter(Files::isRegularFile)  // We only want the files, not directories
        .forEach(sourcePath -> {
            try {
                // Compute the relative path of the object file
                Path relativePath = sourceObjectsPath.relativize(sourcePath);
                
                // Determine the corresponding path in the target repository
                Path targetPath = targetObjectsPath.resolve(relativePath);
                
                // Check if the object already exists at the target path
                if (Files.exists(targetPath)) {
                    //IoUtils.INSTANCE.trace("Object already exists at " + targetPath.toString() + ", skipping copy.");
                    return;  // Skip copying if the object already exists
                }

                // Create the target directories if they don't exist
                Files.createDirectories(targetPath.getParent());
                
                // Copy the object file to the target path, replacing any existing files
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                //IoUtils.INSTANCE.trace("Copied object file from " + sourcePath.toString() + " to " + targetPath.toString());
                
            } catch (IOException e) {
                IoUtils.INSTANCE.fatal("Failed to copy object: " + sourcePath.toString() + " - " + e.getMessage());
            }
            });
    }


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
            Iterable<RevCommit> commits = git.log().add(repository.resolve(src)).call();
        
            // Create a stream from the Iterable
            objects = StreamSupport.stream(commits.spliterator(), false)
                .map(RevCommit::getId)
                .collect(Collectors.toList());
                
        } catch (GitAPIException | IOException e) {
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
        while (!pool.isTerminated()) { }

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


    private void putObject(final ObjectId sha) throws IOException {
        // Determine the path of the object file in the original repository
        Path sourcePath = Paths.get(repository.getDirectory().toString(), 
                                    AppConfig.GIT_OBJECTS, 
                                    sha.name().substring(0, 2), 
                                    sha.name().substring(2));
    
        // Determine the path in the temporary repository where the object file should be copied
        Path targetPath = Paths.get(tmpRepoPath, 
                                    AppConfig.GIT_OBJECTS, 
                                    sha.name().substring(0, 2), 
                                    sha.name().substring(2));

        try {
            // Create directories if they do not exist
            Files.createDirectories(targetPath.getParent());

            // Copy the object file from the source path to the target path
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Optional: Trace the copied file path
            //IoUtils.INSTANCE.trace("Copied object file from " + sourcePath.toString() + " to " + targetPath.toString());

        } catch (IOException e) {
            e.printStackTrace();
            IoUtils.INSTANCE.fatal("putObject ERROR: " + e.getMessage());
        }
    }

    /*public void fetch(final String sha, final String refName) throws IOException {
        // Download the object specified by sha
        download(sha);

        // Update the reference in the new Git directory
        Path refPath = Paths.get(repoPath, refName);
        Files.createDirectories(refPath.getParent());

        // Write the SHA to the reference file
        Files.write(refPath, sha.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

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

    
    private List<Ref> getRefs() {
        try {
            return tmpGit.branchList().call();
        } catch (GitAPIException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private String writeRef(String sha, String ref, boolean force) {
        Ref oldRef = refs.get(ref);
        if (oldRef != null) {
            try {
                // Convert SHA to ObjectId                                                                                                                       
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
                // Use the correct method to update ref                                                                                                              
                RefUpdate refUpdate = tmpRepository.updateRef(ref);
                refUpdate.setNewObjectId(ObjectId.fromString(sha));
                refUpdate.setForceUpdate(force);  // Set force update if necessary                                                                                    
                RefUpdate.Result result = refUpdate.update();  // Use update() instead of call()

                // Check result                                                                                                                                      
                if (result == RefUpdate.Result.NEW || result == RefUpdate.Result.FORCED
                    || result == RefUpdate.Result.FAST_FORWARD ) {
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

    private String calculateRepoSignature(boolean includeCurrentCommit) {
        try (RevWalk revWalk = new RevWalk(repository)) {
            Iterable<RevCommit> commits = git.log().call();
            
            List<String> commitHashes = StreamSupport.stream(commits.spliterator(), false)
                .skip(includeCurrentCommit ? 0 : 1)
                .map(RevCommit::getId)
                .map(ObjectId::getName)
                .sorted()
                .collect(Collectors.toList());
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String hash : commitHashes) {
                digest.update(hash.getBytes());
            }
            byte[] combinedHashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : combinedHashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | GitAPIException e) {
            return null;
        }
    }
    
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
}
