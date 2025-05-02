package ie.dcu.secureYAC;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ServerThread extends Thread {
    private ServerSocket serverSocket;
    private Set<ServerThreadThread> serverThreadThreads = new HashSet<ServerThreadThread>();
    private Consumer<FileTransferData> fileTransferHandler;

    public ServerThread(String port) throws IOException {
        serverSocket = new ServerSocket(Integer.valueOf(port));
    }

    public void run() {
        try {
            while (true) {
                ServerThreadThread serverThreadThread = new ServerThreadThread(serverSocket.accept(), this);
                serverThreadThreads.add(serverThreadThread);
                serverThreadThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendMessage(String message) {
        try {
            serverThreadThreads.forEach(t -> t.getPrintWriter().println(message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFileTransferHandler(Consumer<FileTransferData> handler) {
        this.fileTransferHandler = handler;
        serverThreadThreads.forEach(t -> t.setFileTransferHandler(handler));
    }

    public Consumer<FileTransferData> getFileTransferHandler() {
        return fileTransferHandler;
    }

    public Set<ServerThreadThread> getServerThreadThreads() {
        return serverThreadThreads;
    }
}
