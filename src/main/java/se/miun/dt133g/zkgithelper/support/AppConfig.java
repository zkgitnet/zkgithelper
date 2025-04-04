package se.miun.dt133g.zkgithelper.support;

/**
 * Defines application-wide constants used throughout the ZkGitHelper system.
 * This utility class contains string constants for Git-related commands,
 * status and error messages, file system configurations, and formatting symbols.
 * It is not intended to be instantiated.
 * @author Leif Rogell
 */
public final class AppConfig {

    private AppConfig() {
        throw new IllegalStateException("Utility class");
    }

    // ZK Git commands
    public static final String COMMAND_SUCCESS = "SUCCESS";

    public static final String ERROR_KEY = "ERROR";

    public static final String COMMAND_STATUS = "STATUS";

    public static final String COMMAND_SEND = "SEND";

    public static final String COMMAND_REQUEST = "REQUEST";

    public static final String COMMAND_CLEAN = "CLEAN";

    // Paths configuration
    public static final String JAVA_TMP = "java.io.tmpdir";

    public static final String TMP_PREFIX = "zkgit-tmp-";

    public static final String REFS_PATH = "refs";

    public static final String ZIP_SUFFIX = ".zip";

    // Git commands
    public static final String GIT_CAPABILITIES = "capabilities";

    public static final String GIT_PUSH = "push";

    public static final String GIT_FOR_PUSH = "for-push";

    public static final String GIT_FETCH = "fetch";

    public static final String GIT_LIST = "list";

    public static final String GIT_HEAD = "HEAD";

    public static final String GIT_END = "";

    public static final String GIT_OK = "ok ";

    public static final String GIT_ERROR = "error ";

    public static final String GIT_FORCE = "+";

    public static final String GIT_OBJECTS = "objects";

    public static final String GIT_REF = "ref: ";

    // Connection configuration
    public static final String CONN_LOCALHOST = "localhost";

    // Error messages
    public static final String ERROR_CLIENT_NOT_RUNNING = "ZK Git - client port not open -"
        + " client not running or wrong port configured";

    public static final String ERROR_UNSUPPORTED_OPERATION = "unsupported operation: ";

    public static final String ERROR_REMOTE_BRANCH_FAIL = "failed to set default branch on remote";

    public static final String ERROR_FIRST_REMOTE_BRANCH_FAIL = "first push but no branch to set remote HEAD";

    public static final String ERROR = "error: ";

    public static final String INFO = "info: ";

    public static final String DEBUG = "debug: ";

    public static final String ERROR_CREATE_DIRECTORIES = "error creating directories";

    public static final String ERROR_CREATE_TMP_DIRECTORY = "failed to create temporary directory: ";

    public static final String ERROR_HASH_MISMATCH = "Hash mismatch ";

    public static final String ERROR_OPERATION_INTERRUPTED = "operation interrupted";

    public static final String ERROR_COMPRESSING_FAILED = "Could not compress repository: ";

    public static final String ERROR_DECOMPRESSING_FAILED = "Could not decompress repository: ";

    public static final String ERROR_EMPTY_REPOSITORY = "Repository is empty";

    public static final String ERROR_UNEXPECTED_FILE_FORMAT = "Unexpected format in file: ";

    public static final String ERROR_FILE_NOT_FOUND = "File not found: ";

    public static final String ERROR_READING_FILE = "Error reading file: ";

    public static final String ERROR_WRITING_FILE = "Error writing file: ";

    public static final String ERROR_NON_FAST_FORWARD = "Non-fast-forward error";

    public static final String ERROR_SERVER_NOT_CONNECTED = "ZK Git - Server not reachable: ";

    public static final String ERROR_REPO_TRANSFER_FAILED = "ZK Git - ERROR - Repo could not be transferred to remote";

    public static final String ERROR_CANNOT_WRITE_REF = "Cannot write ref";

    // Status messages
    public static final String STATUS_CLIENT_RUNNING = "ZK Client - Client and Server running and connected";

    public static final String STATUS_SEND_BEGIN = "ZK Git - encrypting and transferring to remote";

    public static final String STATUS_SEND_FINISH = "ZK Git - encryption and transfer completed";

    public static final String STATUS_REQUEST_BEGIN = "ZK Git - retrieving from remote and decrypting";

    public static final String STATUS_REQUEST_FINISH = "ZK Git - retrieval and decryption completed";

    public static final String STATUS_REQUEST_UPTODATE = "ZK Git - already up to date";

    public static final String STATUS_ZKGIT_START = "ZK Git - start";

    public static final String STATUS_ZKGIT_FINISH = "ZK Git - completed";

    public static final String STATUS_NO_DEFAULT_BRANCH = "no default branch on remote";

    public static final String STATUS_DOWNLOADED = "downloaded: ";

    public static final String STATUS_FETCH_FIRST = "fetch first";

    public static final String STATUS_NON_FF = "non-fast forward";

    public static final String STATUS_REPO_UPTODATE = "upToDate";

    public static final String STATUS_COMPRESSING_START = "ZK Git - compressing repository";

    public static final String STATUS_COMPRESSING_FINISH = "ZK Git - compression completed";

    public static final String STATUS_DECOMPRESSING_START = "ZK Git - decompressing repository";

    public static final String STATUS_DECOMPRESSING_FINISH = "ZK Git - decompression completed";

    public static final String STATUS_ENCRYPTING_START = "ZK Git - encrypting repository";

    public static final String STATUS_ENCRYPTING_FINISH = "ZK Git - encryption completed";

    public static final String STATUS_DECRYPTING_START = "ZK Git - decrypting repository";

    public static final String STATUS_DECRYPTING_FINISH = "ZK Git - decryption completed";

    public static final String STATUS_TRANSFER_START = "ZK Git - transferring encrypted repository";

    public static final String STATUS_TRANSFER_FINISH = "ZK Git - encrypted transfer completed";

    public static final String STATUS_RETRIEVE_START = "ZK Git - retrieving encrypted repository";

    public static final String STATUS_RETRIEVE_FINISH = "ZK Git - encrypted retrieval completed";

    public static final String STATUS_CLEANING_TMP = "ZK Git - cleaning temporary files";

    public static final String STATUS_BEGIN_PUSH = "ZK Git - preparing push";

    public static final String STATUS_BEGIN_PULL = "ZK Git - preparing pull";

    // Other
    public static final String SPACE_SEPARATOR = " ";

    public static final String SLASH_SEPARATOR = "/";

    public static final String DOT_SEPARATOR = ".";

    public static final String AT_SEPARATOR = "@";

    public static final String COLON_SEPARATOR = ":";

    public static final String NEW_LINE = System.lineSeparator();

    public static final int MAX_NUM_PROCESSES = 5;

    public static final String ERASE = "\033[F\033[K";

    public static final int ONE_KB = 1024;
}
