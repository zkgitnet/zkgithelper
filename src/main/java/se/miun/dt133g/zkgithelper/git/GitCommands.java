package se.miun.dt133g.zkgithelper.git;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Constants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.URISyntaxException;

/**
 * Utility class for common Git operations using JGit.
 * @author Leif Rogell
 */
public final class GitCommands {

    private static final String EMPTY_TREE_HASH = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

    /**
     * Constructor for the GitCommands class.
     */
    public GitCommands() { }

    /**
     * Checks if one commit is an ancestor of another.
     * @param ancestorId the possible ancestor commit ID
     * @param refId the target commit ID
     * @param repository the repository to check in
     * @return true if ancestorId is an ancestor of refId
     * @throws IOException if Git objects cannot be read
     */
    protected boolean isAncestor(final ObjectId ancestorId,
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

    /**
     * Checks if a Git object with the given SHA exists.
     * @param sha the SHA-1 hash
     * @param repository the repository to search
     * @return true if the object exists
     * @throws IOException if the repository cannot be accessed
     */
    protected boolean objectExists(final String sha,
                                final Repository repository)
        throws IOException {
        ObjectId objectId = repository.resolve(sha);
        return objectId != null
            && repository.getObjectDatabase().has(objectId);
    }

    /**
     * Checks if commit history exists for a given SHA.
     * @param sha the SHA-1 of the commit
     * @param repository the repository to check
     * @return true if history exists
     * @throws IOException if the commit cannot be resolved
     */
    protected boolean historyExists(final String sha,
                                 final Repository repository)
        throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            ObjectId commitId = repository.resolve(sha);
            if (commitId == null) {
                return false;
            }
            walk.markStart(walk.parseCommit(commitId));
            return walk.next() != null;
        }
    }

    /**
     * Gets the SHA-1 value of a ref.
     * @param ref the ref name (e.g., refs/heads/main)
     * @param repository the repository to look in
     * @return SHA-1 string or null if not found
     * @throws IOException if the ref cannot be resolved
     */
    protected String refValue(final String ref,
                           final Repository repository)
        throws IOException {
        ObjectId refId = repository.resolve(ref);
        return refId != null ? refId.name() : null;
    }

    /**
     * Gets the target of a symbolic reference.
     * @param name the symbolic ref name (e.g., HEAD)
     * @param repository the repository to query
     * @return the target ref name or null
     * @throws IOException if the ref cannot be read
     */
    protected String symbolicRefValue(final String name,
                                   final Repository repository)
        throws IOException {
        Ref ref = repository.findRef(name);
        return ref != null && ref.isSymbolic()
            ? ref.getTarget().getName()
            : null;
    }

    /**
     * Gets the type of a Git object (commit, tree, blob, tag).
     * @param sha the SHA-1 hash of the object
     * @param repository the repository to check
     * @return the object type as a string or null
     * @throws IOException if the object cannot be parsed
     */
    protected String objectKind(final String sha,
                             final Repository repository)
        throws IOException {
        ObjectId objectId = repository.resolve(sha);
        if (objectId == null) {
            return null;
        }

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevObject revObject = revWalk.parseAny(objectId);
            int type = revObject.getType();
            return Constants.typeString(type);
        }
    }

    /**
     * Gets the raw byte data of a Git object.
     * @param sha the SHA-1 of the object
     * @param kind the object type (unused)
     * @param repository the repository to read from
     * @return the object data or null
     * @throws IOException if the object cannot be read
     */
    protected byte[] objectData(final String sha,
                             final String kind,
                             final Repository repository)
        throws IOException {
        ObjectId objectId = repository.resolve(sha);
        if (objectId == null) {
            return null;
        }

        ObjectLoader loader = repository.open(objectId);
        try {
            return loader.getBytes();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the URL of a remote by name.
     * @param name the remote name (e.g., origin)
     * @param repository the repository to check
     * @return the remote URL or null
     * @throws IOException if the remote cannot be parsed
     */
    protected String getRemoteUrl(final String name,
                               final Repository repository)
        throws IOException {
        try {
            RemoteConfig config =
                new RemoteConfig(repository.getConfig(),
                                                   name);
            return config.getURIs().isEmpty()
                ? null
                : config.getURIs().get(0).toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IOException("Invalid URL syntax for remote: "
                                  + name, e);
        }
    }

    /**
     * Lists all Git object IDs reachable from a ref, excluding given objects.
     * @param ref the starting ref (e.g., branch or commit)
     * @param exclude list of object IDs to skip
     * @param repository the repository to walk
     * @return list of reachable object IDs
     * @throws IOException if traversal fails
     */
    protected List<String> listObjects(final String ref,
                                    final List<String> exclude,
                                    final Repository repository)
        throws IOException {
        List<String> objects = new ArrayList<>();
        try (RevWalk walk = new RevWalk(repository)) {
            ObjectId commitId = repository.resolve(ref);
            walk.markStart(walk.parseCommit(commitId));
            for (RevCommit commit : walk) {
                TreeWalk treeWalk = new TreeWalk(repository);
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    String objectId = treeWalk.getObjectId(0).name();
                    if (!exclude.contains(objectId)) {
                        objects.add(objectId);
                    }
                }
            }
        }
        return objects;
    }

    /**
     * Gets the tree and parent commits referenced by a commit.
     * @param sha the SHA-1 of the commit
     * @param repository the repository to inspect
     * @return list of referenced object SHAs
     * @throws IOException if parsing fails
     */
    protected List<String> referencedObjects(final String sha,
                                          final Repository repository)
        throws IOException {
        List<String> objs = new ArrayList<>();
        ObjectId objectId = repository.resolve(sha);
        if (objectId == null)  {
            return objs;
        }
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            objs.add(commit.getTree().name());
            for (RevCommit parent : commit.getParents()) {
                objs.add(parent.name());
            }
        }
        return objs;
    }
}
