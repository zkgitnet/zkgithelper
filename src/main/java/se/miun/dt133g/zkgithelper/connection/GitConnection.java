package se.miun.dt133g.zkgithelper.connection;

import se.miun.dt133g.zkgithelper.support.IoUtils;
import se.miun.dt133g.zkgithelper.support.AppConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GitConnection {

    public static final GitConnection INSTANCE = new GitConnection();

    private int srcPort;
    private int dstPort;

    private GitConnection() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            this.srcPort = serverSocket.getLocalPort();
        } catch (IOException e) {
            this.srcPort = 0;
        }
    }

    public void ensureConnected() {
        if (!isPortOpen()) {
            IoUtils.INSTANCE.fatal(AppConfig.ERROR_CLIENT_NOT_RUNNING);
        }
        String serverStatus = checkServerStatus();
        if (serverStatus.equals(AppConfig.COMMAND_SUCCESS)) {
            IoUtils.INSTANCE.trace(AppConfig.STATUS_CLIENT_RUNNING);
        } else {
            IoUtils.INSTANCE.fatal(AppConfig.ERROR_SERVER_NOT_CONNECTED
                                   + serverStatus
                                   + AppConfig.NEW_LINE);
        }
    }

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

    private boolean isPortOpen() {
        try (Socket socket = new Socket(AppConfig.CONN_LOCALHOST,
                                        dstPort)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

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

    public String sendFile(final String filePath, final String signature) {
        try (Socket socket = new Socket(AppConfig.CONN_LOCALHOST, dstPort);
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true);
             BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {

            IoUtils.INSTANCE.trace(AppConfig.STATUS_SEND_BEGIN);
            writer.println(AppConfig.COMMAND_SEND + " " + filePath + " " + signature);

            String serverResponse = reader.readLine();
            return serverResponse;

        } catch (UnknownHostException e) {
            System.err.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
        return "Unknown status";
    }

    public String requestFile(final String fileName, final String signature) {
        try (Socket socket = new Socket(AppConfig.CONN_LOCALHOST,
                                        dstPort);
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true);
             BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {

            IoUtils.INSTANCE.trace(AppConfig.STATUS_REQUEST_BEGIN);
            writer.println(AppConfig.COMMAND_REQUEST + " " + fileName + " " + signature);

            String serverResponse = reader.readLine();

            if (serverResponse.contains(AppConfig.COMMAND_SUCCESS)) {
                IoUtils.INSTANCE.trace(AppConfig.STATUS_REQUEST_FINISH);
            } else {
                if (serverResponse.contains(AppConfig.STATUS_REPO_UPTODATE)) {
                    IoUtils.INSTANCE.trace(AppConfig.STATUS_REQUEST_UPTODATE);
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
