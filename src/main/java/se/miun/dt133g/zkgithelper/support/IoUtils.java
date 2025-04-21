package se.miun.dt133g.zkgithelper.support;

import java.util.Scanner;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;

/**
 * Utility class for input/output operations in the ZkGitHelper application.
 * Handles standard input and output, filters unwanted log output (e.g., SLF4J warnings),
 * and provides simple logging and fatal error handling.
 * @author Leif Rogell
 */
public final class IoUtils {

    public static final IoUtils INSTANCE = new IoUtils();
    private static Scanner scanner = new Scanner(System.in);

    private IoUtils() { }

    /**
     * Custom output stream that filters out lines containing unwanted log prefixes (e.g., SLF4J).
     */
    private static final class FilteringOutputStream extends OutputStream {
        private final PrintStream originalErr = System.err;
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void write(final int b) throws IOException {
            if (b == '\n') {
                String message = buffer.toString();
                buffer.setLength(0);
                if (!message.contains("SLF4J")) {
                    originalErr.print(message + (char) b);
                }
            } else {
                buffer.append((char) b);
            }
        }
    }

    // Static initializer: installs filtered System.err stream
    static {
        System.setErr(new PrintStream(new FilteringOutputStream()));
    }

    /**
     * Returns a shared {@link Scanner} instance for reading standard input.
     * @return the scanner connected to System.in
     */
    public Scanner getScanner() {
        return scanner;
    }

    /**
     * Prints a line of output to standard out.
     * @param line the line to print
     */
    public void write(final String line) {
        System.out.println(line);
    }

    /**
     * Logs a trace/debug message to standard error.
     * @param message the message to log
     */
    public void trace(final String message) {
        stderr(message);
    }

    /**
     * Logs a fatal error message and exits the program.
     * @param message the error message to log
     */
    public void fatal(final String message) {
        if (message != null) {
            trace(AppConfig.ERROR + message);
        }
        System.exit(1);
    }

    /**
     * Writes a message to standard error with a line separator.
     * @param message the message to write
     */
    private void stderr(final String message) {
        System.err.print(message + AppConfig.NEW_LINE);
    }
}
