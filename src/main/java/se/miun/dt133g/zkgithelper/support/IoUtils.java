package zkgithelper.support;

import java.util.Scanner;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;

public final class IoUtils {

    public static final IoUtils INSTANCE = new IoUtils();
    private static Scanner scanner = new Scanner(System.in);

    private IoUtils() { }

    private final static class FilteringOutputStream extends OutputStream {
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

    static {
        System.setErr(new PrintStream(new FilteringOutputStream()));
    }

    public Scanner getScanner() {
        return scanner;
    }

    public void write(final String line) {
        System.out.println(line);
    }

    public void trace(final String message) {
        stderr(message);
    }

    public void fatal(final String message) {
        trace(message);
        System.exit(1);
    }

    private void stderr(final String message) {
        System.err.print(message + AppConfig.NEW_LINE);
    }
}
