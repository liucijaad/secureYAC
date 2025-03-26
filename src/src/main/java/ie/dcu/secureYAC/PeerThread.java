package ie.dcu.secureYAC;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.function.Consumer;

public class PeerThread extends Thread {
    private BufferedReader bufferedReader;
    private Consumer<String> messageHandler;
    private Consumer<FileTransferData> fileTransferHandler;
    private Socket socket;

    public PeerThread(Socket socket) throws IOException {
        this.socket = socket;
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public void setFileTransferHandler(Consumer<FileTransferData> handler) {
        this.fileTransferHandler = handler;
    }

    public Socket getSocket() {
        return socket;
    }

    public void run() {
        boolean flag = true;
        while (flag) {
            try {
                String line = bufferedReader.readLine();
                if (line != null && !line.isEmpty()) {
                    JSONObject jsonObject = new JSONObject(line);

                    // Check if this is a file transfer message
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
                    } else {
                        // Regular chat message
                        if (messageHandler != null) {
                            messageHandler.accept(line);
                        } else {
                            // Legacy console output when no handler is set
                            if (jsonObject.has("username")) {
                                System.out.println("[" + jsonObject.getString("username") + "]: " + jsonObject.getString("message"));
                            }
                        }
                    }
                }
            } catch(Exception e) {
                flag = false;
                interrupt();
            }
        }
    }
}