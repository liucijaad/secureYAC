package ie.dcu.secureYAC;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class PeerThread extends Thread {
    private BufferedReader bufferedReader;
    public PeerThread(Socket socket) throws IOException {
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void run() {
        boolean flag = true;
        while (flag) {
            try {
                String line = bufferedReader.readLine();
                if (line != null && !line.isEmpty()) {
                    JSONObject jsonObject = new JSONObject(line);
                    if (jsonObject.has("username")) {
                        System.out.println("[" + jsonObject.getString("username") + "]: " + jsonObject.getString("message"));
                    }
                }
            } catch(Exception e) {
                flag = false;
                interrupt();
            }
        }
    }
}
