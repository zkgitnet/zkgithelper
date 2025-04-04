package se.miun.dt133g.zkgithelper.connection;

import se.miun.dt133g.zkgithelper.support.IoUtils;
import se.miun.dt133g.zkgithelper.support.AppConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles socket-based communication between the ZkGit client and server.
 * Manages port connections, file transfers, and status checks.
 * Implements a singleton pattern via the {@code INSTANCE} field.
 * @author Leif Rogell
 */
public final class GitConnection {

    public static final GitConnection INSTANCE = new GitConnection();

    private int srcPort;
    private int dstPort;

    /**
     * Constructor that allocates an available source port for communication.
     */
    private GitConnection() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            this.srcPort = serverSocket.getLocalPort();
        } catch (IOException e) {
            this.srcPort = 0;
        }
    }

    /**
     * Ensures a connection to the server is established and responds correctly.
     * Terminates the application if connection fails or server is unresponsive.
     */
    public void ensureConnected() {
        if (!isPortOpen()) {
            IoUtils.INSTANCE.fatal(AppConfig.ERROR_CLIENT_NOT_RUNNING);
        }
        String serverStatus = checkServerStatus();
        if (serverStatus.contains(AppConfig.COMMAND_SUCCESS)) {
            IoUtils.INSTANCE.trace(AppConfig.STATUS_CLIENT_RUNNING);
        } else {
            IoUtils.INSTANCE.fatal(AppConfig.ERROR_SERVER_NOT_CONNECTED
                                   + serverStatus
                                   + AppConfig.NEW_LINE);
        }
    }

    /**
     * Sends a STATUS command to the server and returns its response.
     * @return Server response as a string, or "Unknown status" on failure.
     */
    private String checkServerStatus() {
        try (Socket socket = new Socket(AppConfig.CONN_LOCALHOST,
                                        dstPort);
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true);
             InputStream input = socket.getInputStream();
             BufferedReader reader = new BufferedReader(
                new InputStreamReader(input))) {

            writer.println(AppConfig.COMMAND_STATUS);
            String serverResponse = reader.readLine();
            return serverResponse;

        } catch (UnknownHostException e) {
            System.err.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
        return "Unknown status";
    }

    /**
     * Attempts to open a socket to the destination port to verify connectivity.
     * @return {@code true} if the port is reachable, otherwise {@code false}.
     */
    private boolean isPortOpen() {
        try (Socket socket = new Socket(AppConfig.CONN_LOCALHOST,
                                        dstPort)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Parses the destination port number from the Git URL and stores it.
     * @param gitUrl Git remote URL in the format {@code user@host:port/repo}.
     */
    public void setDstPort(final String gitUrl) {
        String regex = ".*:(\\d+)/.*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(gitUrl);

        if (matcher.matches()) {
            String portString = matcher.group(1);
            this.dstPort = Integer.parseInt(portString);
        } else {
            this.dstPort = 0;
        }
    }

    /**
     * Sends a file to the server using the SEND command.
     * @param filePath Path to the file to send.
     * @param signature A unique identifier for the file (e.g., a hash or tag).
     * @return Response from the server, or "Unknown status" on error.
     */
    public String sendFile(final String filePath, final String signature) {
        try (Socket socket = new Socket(AppConfig.CONN_LOCALHOST, dstPort);
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true);
             BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {

            IoUtils.INSTANCE.trace(AppConfig.STATUS_SEND_BEGIN);
            writer.println(AppConfig.COMMAND_SEND + " " + filePath + " " + signature);

            String serverResponse = reader.readLine();
            IoUtils.INSTANCE.trace("serverResponse: " + serverResponse);
            return serverResponse;

        } catch (UnknownHostException e) {
            System.err.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
        return "Unknown status";
    }

    /**
     * Requests a file from the server using the REQUEST command.
     * @param fileName Name of the file to retrieve.
     * @param signature Identifier for file version or target state.
     * @return Server response, including success or uptodate status.
     */
    public String requestFile(final String fileName, final String signature) {
        try (Socket socket = new Socket(AppConfig.CONN_LOCALHOST,
                                        dstPort);
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true);
             BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {

            IoUtils.INSTANCE.trace(AppConfig.STATUS_REQUEST_BEGIN + " " + fileName + " " + signature);
            writer.println(AppConfig.COMMAND_REQUEST + " " + fileName + " " + signature);

            String serverResponse = reader.readLine();
            IoUtils.INSTANCE.trace("serverResponse: " + serverResponse);

            if (serverResponse.contains(AppConfig.COMMAND_SUCCESS)) {
                if (serverResponse.contains(AppConfig.STATUS_REPO_UPTODATE)) {
                    IoUtils.INSTANCE.trace(AppConfig.STATUS_REQUEST_UPTODATE);
                } else {
                    IoUtils.INSTANCE.trace(AppConfig.STATUS_REQUEST_FINISH);
                }
            }

            return serverResponse;

        } catch (UnknownHostException e) {
            System.err.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
        return "Unknown status";
    }

    /**
     * Instructs the server to clean up temporary files related to a specific repository.
     * @param repoName Name of the repository whose temp data should be removed.
     * @return Server response after attempting cleanup.
     */
    public String cleanTmp(final String repoName) {
        try (Socket socket = new Socket(AppConfig.CONN_LOCALHOST, dstPort);
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true);
             BufferedReader reader = new BufferedReader(
                                                        new InputStreamReader(socket.getInputStream()))) {

            IoUtils.INSTANCE.trace(AppConfig.STATUS_CLEANING_TMP);
            writer.println(AppConfig.COMMAND_CLEAN
                           + " "
                           + repoName);

            String serverResponse = reader.readLine();
            return serverResponse;

        } catch (UnknownHostException e) {
            System.err.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
        return "Unknown status";
    }
}
