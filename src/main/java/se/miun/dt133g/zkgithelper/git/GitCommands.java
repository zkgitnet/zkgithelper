package zkgithelper.git;

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

public final class GitCommands {

    private static final String EMPTY_TREE_HASH = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

    public GitCommands() { }

    /*public boolean isAncestor(final String ancestor,
                              final String ref,
                              final Repository repository)
        throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            ObjectId ancestorId = repository.resolve(ancestor);
            ObjectId refId = repository.resolve(ref);
            RevCommit ancestorCommit = walk.parseCommit(ancestorId);
            RevCommit refCommit = walk.parseCommit(refId);
            return walk.isMergedInto(ancestorCommit, refCommit);
        }
        }*/

    public boolean isAncestor(final ObjectId ancestorId,
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

    public boolean objectExists(final String sha,
                                final Repository repository)
        throws IOException {
        ObjectId objectId = repository.resolve(sha);
        return objectId != null
            && repository.getObjectDatabase().has(objectId);
    }

    public boolean historyExists(final String sha,
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

    public String refValue(final String ref,
                           final Repository repository)
        throws IOException {
        ObjectId refId = repository.resolve(ref);
        return refId != null ? refId.name() : null;
    }

    public String symbolicRefValue(final String name,
                                   final Repository repository)
        throws IOException {
        Ref ref = repository.findRef(name);
        return ref != null && ref.isSymbolic()
            ? ref.getTarget().getName()
            : null;
    }

    public String objectKind(final String sha,
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

    public byte[] objectData(final String sha,
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
        } finally {
        }
    }

    public String getRemoteUrl(final String name,
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

    public List<String> listObjects(final String ref,
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

    public List<String> referencedObjects(final String sha,
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
