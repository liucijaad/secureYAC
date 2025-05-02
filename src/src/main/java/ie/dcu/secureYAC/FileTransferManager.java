package ie.dcu.secureYAC;

import javafx.application.Platform;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Manager class to handle file transfer operations
 */
public class FileTransferManager {
    private final int MAX_CHUNK_SIZE = 64 * 1024; // 64KB chunks
    private Map<String, FileTransferProgress> incomingTransfers;
    private Consumer<String> onFileReceived;
    private Consumer<String> onTransferProgress;
    private String downloadDirectory;

    public FileTransferManager() {
        incomingTransfers = new HashMap<>();

        // Create downloads directory if it doesn't exist
        downloadDirectory = System.getProperty("user.home") + File.separator + "SecureYAC-Downloads";
        try {
            Files.createDirectories(Paths.get(downloadDirectory));
        } catch (IOException e) {
            System.err.println("Could not create downloads directory: " + e.getMessage());
        }
    }

    public void setOnFileReceived(Consumer<String> callback) {
        this.onFileReceived = callback;
    }

    public void setOnTransferProgress(Consumer<String> callback) {
        this.onTransferProgress = callback;
    }

    public void handleIncomingFileTransfer(FileTransferData fileData) {
        String transferId = fileData.getSender() + "-" + fileData.getFileName();

        // If this is the first chunk, create a new progress tracker
        if (!incomingTransfers.containsKey(transferId)) {
            incomingTransfers.put(transferId, new FileTransferProgress(
                    fileData.getFileName(),
                    fileData.getFileSize(),
                    fileData.getTotalChunks()
            ));

            notifyProgress(fileData.getSender(), fileData.getFileName(), 0);
        }

        // Get the progress tracker and add this chunk
        FileTransferProgress progress = incomingTransfers.get(transferId);
        progress.addChunk(fileData.getChunkIndex(), fileData.getDecodedData());

        // Calculate progress percentage
        int progressPercent = (int)((progress.getReceivedChunks() * 100.0) / progress.getTotalChunks());

        // Only notify about progress at certain intervals (0%, 5%, 10%...) to reduce message spam
        if (progressPercent % 5 == 0 || progressPercent == 100) {
            notifyProgress(fileData.getSender(), fileData.getFileName(), progressPercent);
        }

        // If the transfer is complete, save the file
        if (progress.isComplete()) {
            saveCompletedFile(fileData.getSender(), fileData.getFileName(), progress);
            incomingTransfers.remove(transferId);
        }
    }

    private void saveCompletedFile(String sender, String fileName, FileTransferProgress progress) {
        try {
            // Create file in downloads directory
            String filePath = downloadDirectory + File.separator + fileName;

            // Write the file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(progress.getCompleteFileData());
            }

            // Notify listeners
            if (onFileReceived != null) {
                Platform.runLater(() -> onFileReceived.accept("File received from " + sender + ": " + fileName));
            }
        } catch (IOException e) {
            System.err.println("Failed to save received file: " + e.getMessage());
        }
    }

    private void notifyProgress(String sender, String fileName, int percentage) {
        if (onTransferProgress != null) {
            Platform.runLater(() -> onTransferProgress.accept(
                    "Receiving file from " + sender + ": " + fileName + " (" + percentage + "%)"
            ));
        }
    }

    public JSONObject[] prepareFileTransfer(String username, File file) throws IOException {
        byte[] fileData = Files.readAllBytes(file.toPath());

        // Calculate number of chunks needed
        int totalChunks = (int) Math.ceil((double) fileData.length / MAX_CHUNK_SIZE);
        JSONObject[] messages = new JSONObject[totalChunks];

        // Create chunked messages
        for (int i = 0; i < totalChunks; i++) {
            int start = i * MAX_CHUNK_SIZE;
            int end = Math.min(start + MAX_CHUNK_SIZE, fileData.length);
            int chunkSize = end - start;

            byte[] chunk = new byte[chunkSize];
            System.arraycopy(fileData, start, chunk, 0, chunkSize);

            // Encode chunk as base64
            String base64Data = Base64.getEncoder().encodeToString(chunk);

            // Create JSON message
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("messageType", "FILE_TRANSFER");
            jsonMessage.put("username", username);
            jsonMessage.put("fileName", file.getName());
            jsonMessage.put("fileData", base64Data);
            jsonMessage.put("fileSize", fileData.length);
            jsonMessage.put("chunkIndex", i);
            jsonMessage.put("totalChunks", totalChunks);

            messages[i] = jsonMessage;
        }

        return messages;
    }

    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    /**
     * Inner class to track file transfer progress
     */
    private static class FileTransferProgress {
        private final int totalChunks;
        private int receivedChunks;
        private final byte[][] chunks;

        public FileTransferProgress(String fileName, long fileSize, int totalChunks) {
            this.totalChunks = totalChunks;
            this.receivedChunks = 0;
            this.chunks = new byte[totalChunks][];
        }

        public void addChunk(int index, byte[] data) {
            if (chunks[index] == null) {
                chunks[index] = data;
                receivedChunks++;
            }
        }

        public boolean isComplete() {
            return receivedChunks == totalChunks;
        }

        public int getReceivedChunks() {
            return receivedChunks;
        }

        public int getTotalChunks() {
            return totalChunks;
        }

        public byte[] getCompleteFileData() {
            // Combine all chunks into one byte array
            int totalSize = 0;
            for (byte[] chunk : chunks) {
                if (chunk != null) {
                    totalSize += chunk.length;
                }
            }

            byte[] result = new byte[totalSize];
            int position = 0;

            for (byte[] chunk : chunks) {
                if (chunk != null) {
                    System.arraycopy(chunk, 0, result, position, chunk.length);
                    position += chunk.length;
                }
            }

            return result;
        }
    }
}