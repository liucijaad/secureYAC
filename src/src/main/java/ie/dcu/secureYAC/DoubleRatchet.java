package ie.dcu.secureYAC;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
    This class represents the Double Ratchet Algorithm. Implementation based on
    <a href="https://signal.org/docs/specifications/doubleratchet/">Signal's Double
    Ratchet Algorithm specification</a>.
 *
 * @author Liucija Paulina Adomaviciute */

public class DoubleRatchet {
    private static final byte[] CK_CONSTANT = new byte[] { 0x01 };
    private static final byte[] MK_CONSTANT = new byte[] { 0x02 };
    private static final int    MAX_SKIP    =                  10;

    private byte[] sharedSecret;
    private byte[] sendingPrivateKey;
    private byte[] receivedKeyPublic;
    private byte[] rootKey;
    private byte[] sendingChainKey;
    private byte[] receivingChainKey;
    private int sendingMessageNo;
    private int receivingMessageNo;
    private int prevSendingChainMessageNo;
    private HashMap<byte[], Integer> messKeySkipped;

    DoubleRatchet(IdentityKeyBundle identity, PreKeyBundle preKeyBundle, byte[] sharedSecret)
        throws NoSuchAlgorithmException {
        this.sharedSecret = sharedSecret;
        this.sendingPrivateKey = identity.getIdentityPrivateKey();
        this.receivedKeyPublic = preKeyBundle.getPreKeyPublic();
        this.rootKey = this.sharedSecret;
        this.sendingChainKey = KDFRootKey(); 
        this.receivingChainKey = null;
        this.sendingMessageNo = 0;
        this.receivingMessageNo = 0;
        this.prevSendingChainMessageNo = 0;
        this.messKeySkipped = new HashMap<>();
    }

    public int getSendingMessageNo() { return this.sendingMessageNo; }
    public int getPrevSendingChainMessageNo() { return this.prevSendingChainMessageNo; }

    /**
        Derive new 32 byte root key using HKDF..
     * @return new 32 byte chain key.
     * @throws NoSuchAlgorithmException */
    private byte[] KDFRootKey() throws NoSuchAlgorithmException {
        byte[] dhValue = DH(this.sendingPrivateKey, this.receivedKeyPublic);
        byte[] hkdfValue = Util.HKDF(this.rootKey, dhValue, "SecureYAC_DR");
        this.rootKey = java.util.Arrays.copyOfRange(hkdfValue, 0, 32);
        return java.util.Arrays.copyOfRange(hkdfValue, 32, 64);
    }

    /**
        Derive a new 32 byte key using HMAC and appropriate constant.
     * @param constant constant used to generate chain or message keys.
     * @param key 32 byte chain key.
     * @return chain or message key, depending on the constant used.
     * @throws NoSuchAlgorithmException */
    private byte[] KDFChainKey(byte[] constant, byte[] key) throws NoSuchAlgorithmException {
        return Util.HMAC(constant, key);
    }

    /**
        Perform Diffie-Helman calculation using given private and public 32 byte keys.
     * @return public key multiplied by privateKey modulus X25519 prime. */
    private byte[] DH(byte[] privateKey, byte[] publicKey) {
        BigInteger privateInt = Util.byteArrayToBigInteger(privateKey);
        BigInteger publicInt = Util.byteArrayToBigInteger(publicKey);
        BigInteger dhValue = publicInt.modPow(privateInt, X25519.PRIME);
        return Util.bigIntToByteArray(dhValue, 32);
    }

    private byte[] header() {
        ByteBuffer tmp = ByteBuffer.allocate(4);
        tmp.putInt(this.prevSendingChainMessageNo);
        byte[] concat = Util.concatByteArrays(this.sendingChainKey,
            Util.changeEndian(tmp.array()));
        tmp.clear();
        tmp.putInt(this.sendingMessageNo);
        return Util.concatByteArrays(concat, Util.changeEndian(tmp.array()));
    }

    /**
        Key derivation function for AES encryption.
     * @throws NoSuchAlgorithmException S*/
    private byte[] AESHKDF(byte[] messageKey) throws NoSuchAlgorithmException {
        byte[] salt = new byte[32];
        for(int i = 0; i < 32; i++) {
            salt[i] = (byte) 0;
        }
        return Util.HKDF(messageKey, salt, "SecureYAC-AES");
    }
    
    public Message ratchetEncrypt(byte[] plaintext) throws Exception {
        this.sendingChainKey = this.KDFChainKey(CK_CONSTANT, this.sendingChainKey);
        byte[] messageKey = this.KDFChainKey(MK_CONSTANT, this.sendingChainKey);
        byte[] hkdfOut = this.AESHKDF(messageKey);
        byte[] authenticationKey = java.util.Arrays.copyOfRange(hkdfOut, 0, 32);
        byte[] iv = java.util.Arrays.copyOfRange(hkdfOut, 32, 48);
        this.sendingMessageNo += 1;
        byte[] AD = Util.HMAC(plaintext, authenticationKey);
        return new Message(this.header(), AD,
            AES(Cipher.ENCRYPT_MODE, messageKey, iv, plaintext));
    }

    public byte[] ratchetDecrypt(Message message) throws Exception {
        message.extractHeader();
        this.receivingChainKey = message.getKey();
        this.trySkippedMessageKeys(message);
        if(message.getPrevMessageNo() != this.prevSendingChainMessageNo) {
            this.skipMessageKeys(message.getPrevMessageNo());
            this.DHRatchet(message);
        }
        this.skipMessageKeys(message.getCurrentMessageNo());
        byte[] messageKey = this.KDFChainKey(MK_CONSTANT, this.receivingChainKey);
        byte[] hkdfOut = this.AESHKDF(messageKey);
        byte[] authenticationKey = java.util.Arrays.copyOfRange(hkdfOut, 0, 32);
        this.receivingMessageNo += 1;
        byte[] iv = java.util.Arrays.copyOfRange(hkdfOut, 32, 48);
        byte[] plaintext = AES(Cipher.DECRYPT_MODE, messageKey,
            iv, message.getCiphertext());
        if(message.verify(plaintext, authenticationKey)) {
            return plaintext;
        }
        throw new Exception();
    }

    /**
        AES encryption in CBC mode using PKCS7 padding.
     * @throws Exception */
    byte[] AES(int mode, byte[] key, byte[] iv, byte[] original)
        throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        Key aesKey = new SecretKeySpec(key, "AES");
        IvParameterSpec ivParam = new IvParameterSpec(iv);
        cipher.init(mode, aesKey, ivParam);
        ByteArrayInputStream bais = new ByteArrayInputStream(original);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[16];
        int readBytes = 0;
        byte[] cipherBlock = new byte[cipher.getOutputSize(16)];
        int cipherBytes;
        int counter = original.length - 1;
        while((readBytes) != -1 & counter != 0) {
            cipherBytes =
                cipher.update(buffer, 0, readBytes, cipherBlock);
            baos.write(cipherBlock, 0, cipherBytes);
            readBytes = bais.read(buffer);
            counter -= readBytes;
        }
        cipherBytes = cipher.doFinal(cipherBlock,0);
        baos.write(cipherBlock,0,cipherBytes);
        return baos.toByteArray();

    }

    private void trySkippedMessageKeys(Message message) {
        if(this.messKeySkipped.containsKey(message.getKey())) {
            this.receivingChainKey = message.getKey();
            this.receivingMessageNo = this.messKeySkipped.get(message.getKey());
            this.messKeySkipped.remove(message.getKey());
        }
    }

    private void skipMessageKeys(int until) throws Exception {
        if(this.receivingMessageNo + MAX_SKIP < until) {
            throw new Exception();
        }
        if(this.receivingChainKey == null) {
            do {
                this.receivingChainKey = KDFChainKey(CK_CONSTANT, this.receivingChainKey);
                byte[] messageKey = KDFChainKey(MK_CONSTANT, this.receivingChainKey);
                this.messKeySkipped.put(messageKey, this.receivingMessageNo);
                this.receivingMessageNo += 1;
            } while(this.receivingMessageNo < until);
        }
    }

    /**
        Diffie-Hellman Ratchet to generate new private and public keys after receiving message.
     * @throws NoSuchAlgorithmException */
    private void DHRatchet(Message message) throws NoSuchAlgorithmException {
        this.prevSendingChainMessageNo = message.getPrevMessageNo();
        this.receivedKeyPublic = message.getKey();
        this.sendingMessageNo = 0;
        this.receivingMessageNo = 0;
        this.sendingChainKey = this.KDFRootKey();
        this.sendingPrivateKey = X25519.generatePrivateKey();
    }
}
