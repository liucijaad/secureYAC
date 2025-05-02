package ie.dcu.secureYAC;

import java.util.Base64;

/**
 * Data class to hold file transfer information
 */
public class FileTransferData {
    private String sender;
    private String fileName;
    private String fileDataBase64;
    private long fileSize;
    private int chunkIndex;
    private int totalChunks;
    private byte[] decodedData;

    public FileTransferData(String sender, String fileName, String fileDataBase64, long fileSize, int chunkIndex, int totalChunks) {
        this.sender = sender;
        this.fileName = fileName;
        this.fileDataBase64 = fileDataBase64;
        this.fileSize = fileSize;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;

        // Decode the base64 data
        try {
            this.decodedData = Base64.getDecoder().decode(fileDataBase64);
        } catch (Exception e) {
            this.decodedData = new byte[0];
        }
    }

    public String getSender() {
        return sender;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileDataBase64() {
        return fileDataBase64;
    }

    public byte[] getDecodedData() {
        return decodedData;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public boolean isLastChunk() {
        return chunkIndex == totalChunks - 1;
    }

    public boolean isComplete() {
        return totalChunks == 1 || isLastChunk();
    }
}