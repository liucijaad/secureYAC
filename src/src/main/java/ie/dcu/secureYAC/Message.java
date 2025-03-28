package ie.dcu.secureYAC;

public class Message {
    private byte[] header;
    private byte[] AD;
    private byte[] ciphertext;
    private byte[] key;
    private Integer currentMessageNo;
    private Integer prevMessageNo;

    Message (byte[] header, byte[] AD, byte[] ciphertext) {
        this.header = header;
        this.AD = AD;
        this.ciphertext = ciphertext;
    }

    byte[] getCiphertext() { return this.ciphertext; }
    byte[] getKey() { return this.key; }
    Integer getCurrentMessageNo() { return this.currentMessageNo; }
    Integer getPrevMessageNo() { return this.prevMessageNo; }
    Integer getHeaderADLength() { return this.header.length + this.AD.length; }

    public void extractHeader() {
        this.key = java.util.Arrays.copyOfRange(this.header, 0, 32);
        this.prevMessageNo =  Util.byteArrayToBigInteger(
            java.util.Arrays.copyOfRange(this.header, 32, 36)).intValue();
        this.currentMessageNo = Util.byteArrayToBigInteger(
            java.util.Arrays.copyOfRange(this.header, 36, 40)).intValue();
    }

    public Boolean verify(byte[] plaintext, byte[] authKey) {
        byte[] AD = Util.HMAC(plaintext, authKey);
        return java.util.Arrays.equals(this.AD, AD);
    }
}
