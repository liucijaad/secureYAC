package ie.dcu.secureYAC;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ServerThreadThread extends Thread {
    private ServerThread serverThread;
    private Socket socket;
    private PrintWriter printWriter;
    private Consumer<FileTransferData> fileTransferHandler;

    public ServerThreadThread(Socket socket, ServerThread serverThread) {
        this.serverThread = serverThread;
        this.socket = socket;
        this.fileTransferHandler = serverThread.getFileTransferHandler();
    }

    public void run() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.printWriter = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                String line = bufferedReader.readLine();
                if (line != null && !line.isEmpty()) {
                    // Check if this is a file transfer message
                    try {
                        JSONObject jsonObject = new JSONObject(line);
                        if (jsonObject.has("messageType") && jsonObject.getString("messageType").equals("FILE_TRANSFER")) {
                            if (fileTransferHandler != null) {
                                // Extract file transfer data
                                FileTransferData fileData = new FileTransferData(
                                        jsonObject.getString("username"),
                                        jsonObject.getString("fileName"),
                                        jsonObject.getString("fileData"),
                                        jsonObject.getLong("fileSize"),
                                        jsonObject.optInt("chunkIndex", 0),
                                        jsonObject.optInt("totalChunks", 1)
                                );
                                fileTransferHandler.accept(fileData);
                            }
                        }
                    } catch (Exception e) {
                        // Not a valid JSON or not a file transfer - continue with normal processing
                    }

                    // Forward the message to all clients
                    serverThread.sendMessage(line);
                }
            }
        } catch (Exception e) {
            serverThread.getServerThreadThreads().remove(this);
        }
    }

    public void setFileTransferHandler(Consumer<FileTransferData> handler) {
        this.fileTransferHandler = handler;
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
    }
}