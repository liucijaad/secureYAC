package ie.dcu.secureYAC;

/**
 *
    This class represents a message sent between users and stores the
    required information for decryption.
 *
 * @author Liucija Paulina Adomaviciute
 */

public class Message {
    private byte[] header;
    private byte[] AD;
    private byte[] ciphertext;
    private byte[] key;
    private Integer currentMessageNo;
    private Integer prevMessageNo;
    private MessageType type;

    enum MessageType {
        TEXT,
        FILE
    }

    Message(MessageType type, byte[] header, byte[] AD, byte[] ciphertext) {
        this.type = type;
        this.header = header;
        this.AD = AD;
        this.ciphertext = ciphertext;
    }

    public byte[] getCiphertext() {
        return this.ciphertext;
    }

    public byte[] getKey() {
        return this.key;
    }

    public Integer getCurrentMessageNo() {
        return this.currentMessageNo;
    }

    public Integer getPrevMessageNo() {
        return this.prevMessageNo;
    }

    public MessageType getMessageType() {
        return this.type;
    }

    void extractHeader() {
        this.key = java.util.Arrays.copyOfRange(this.header, 0, 32);
        this.prevMessageNo = Util.byteArrayToBigInteger(java.util.Arrays.copyOfRange(
                this.header, 32, 36))
                .intValue();
        this.currentMessageNo = Util.byteArrayToBigInteger(
                java.util.Arrays.copyOfRange(this.header, 36, 40))
                .intValue();
    }

    public Boolean verify(byte[] plaintext, byte[] authKey) {
        byte[] AD = Util.HMAC(plaintext, authKey);
        return java.util.Arrays.equals(this.AD, AD);
    }
}
