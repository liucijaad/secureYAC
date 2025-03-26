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
    public PeerThread(Socket socket) throws IOException {
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public void run() {
        boolean flag = true;
        while (flag) {
            try {
                String line = bufferedReader.readLine();
                if (line != null && !line.isEmpty()) {
                    // If we have a message handler, pass the message to it
                    if (messageHandler != null) {
                        messageHandler.accept(line);
                    } else {
                        // Legacy console output when no handler is set
                        JSONObject jsonObject = new JSONObject(line);
                        if (jsonObject.has("username")) {
                            System.out.println("[" + jsonObject.getString("username") + "]: " + jsonObject.getString("message"));
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