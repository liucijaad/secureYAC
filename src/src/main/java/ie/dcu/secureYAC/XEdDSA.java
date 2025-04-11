package ie.dcu.secureYAC;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 *
    This class respresents the XEdDSA signature scheme. Implementation based on
    <a href="https://signal.org/docs/specifications/xeddsa/">Signal's XEdDSA
    specification</a>.
 *
 * @author Liucija Paulina Adomaviciute
 */

public class XEdDSA {
    private static final BigInteger BASE_POINT = new BigInteger(
        "5866666666666666666666666666666666666666666666666666666666666666A", 16);
    private static final BigInteger ORDER = new BigInteger(
        "1000000000000000000000000000000014DEF9DEA2F79CD65812631A5CF5D3ED", 16);

    private byte[] montKey;
    private byte[] publicKey;
    private byte[] privateKey;

    XEdDSA(byte[] montKey) {
        this.montKey = montKey;
    }

    /** Calculate Ed25519 private and public keys from X25519 private key. */
    void calculateKeyPair() {
        this.privateKey = calculatePrivateKey(Util.byteArrayToBigInteger(this.montKey));
        this.publicKey = calculatePublicKey(this.privateKey);
    }

    private static byte[] calculatePublicKey(byte[] privateKey) {
        BigInteger edPrivateKey = Util.byteArrayToBigInteger(privateKey);
        byte[] edPublicKey = Util.bigIntToByteArray(
                edPrivateKey.multiply(BASE_POINT).mod(ORDER), 256);
        // Force sign bit to 0.
        edPublicKey[31] &= 0b01111111;
        return edPublicKey;
    }

    private byte[] calculatePrivateKey(BigInteger montKey) {
        byte[] edPrivateKey = Util.bigIntToByteArray(montKey.mod(ORDER), 256);
        edPrivateKey[31] &= 0b01111111;
        return edPrivateKey;
    }

    public byte[] getPublicKey() {
        return this.publicKey;
    }

    public byte[] getPrivateKey() {
        return this.privateKey;
    }

    /**
        Apply {@link ie.dcu.secureYAC.Util#hash() SHA-512} to data.
     *
     * @param i if i == 1 (hash1), append 0xFE to data.
     * @return SHA-512 hash of input data.
     * @throws NoSuchAlgorithmException
     */
    private byte[] hash(byte[] data, int i) throws NoSuchAlgorithmException {
        if (i == 1) {
            data[0] = (byte) 0xFE;
        }
        return Util.hash(data);
    }

    public static byte[] sign(byte[] identityKey, byte[] keyToSign)
            throws Exception {
        XEdDSA xEdDSA = new XEdDSA(identityKey);
        xEdDSA.calculateKeyPair();
        SecureRandom random = new SecureRandom();
        byte[] randomData = new byte[64];
        random.nextBytes(randomData);
        byte[] byteConcat = Util.concatByteArrays(
                Util.concatByteArrays(xEdDSA.privateKey, keyToSign), randomData);
        byte[] nonce = Util.bigIntToByteArray(
                Util.byteArrayToBigInteger(xEdDSA.hash(byteConcat, 1)).mod(
                                        XEdDSA.ORDER), 256);
        byte[] point = Util.bigIntToByteArray(
                Util.byteArrayToBigInteger(nonce).multiply(BASE_POINT).mod(
                                        XEdDSA.ORDER), 256);
        byteConcat = Util.concatByteArrays(
                Util.concatByteArrays(point, xEdDSA.getPublicKey()), keyToSign);
        byte[] hash = Util.bigIntToByteArray(
                Util.byteArrayToBigInteger(xEdDSA.hash(byteConcat, 0)).mod(
                                        XEdDSA.ORDER), 256);
        // signature = nonce + (hash * privateKey) % ORDER
        byte[] signature = Util.bigIntToByteArray(
                Util.byteArrayToBigInteger(nonce)
                        .add(Util.byteArrayToBigInteger(hash).multiply(
                                Util.byteArrayToBigInteger(xEdDSA.privateKey)))
                        .mod(XEdDSA.ORDER), 256);
        return Util.concatByteArrays(
                xEdDSA.getPublicKey(), Util.concatByteArrays(point, signature));
    }

    public static Boolean verify(PreKeyBundle keyBundle)
            throws NoSuchAlgorithmException {
        XEdDSA xEdDSA = new XEdDSA(keyBundle.getIdentityPublicKey());
        byte[] bundleSignature = keyBundle.getPreKeySignature();
        xEdDSA.publicKey = java.util.Arrays.copyOfRange(bundleSignature, 0, 32);
        byte[] point = java.util.Arrays.copyOfRange(bundleSignature, 32, 64);
        byte[] signature = java.util.Arrays.copyOfRange(bundleSignature, 64, 96);
        if (!XEdDSA.isValidSignature(
                keyBundle.getIdentityPublicKey(), point, signature)) {
            return false;
        }
        byte[] byteConcat = Util.concatByteArrays(
                Util.concatByteArrays(point, xEdDSA.getPublicKey()),
                keyBundle.getPreKeyPublic());
        byte[] hash = Util.bigIntToByteArray(
                Util.byteArrayToBigInteger(xEdDSA.hash(byteConcat, 0))
                        .mod(XEdDSA.ORDER), 256);
        // Rcheck = (signature * BASE_POINT) - (hash * EdPublicKey)
        byte[] Rcheck = Util.bigIntToByteArray(
                (Util.byteArrayToBigInteger(signature).multiply(XEdDSA.BASE_POINT))
                        .subtract(Util.byteArrayToBigInteger(hash).multiply(
                                Util.byteArrayToBigInteger(xEdDSA.getPublicKey())))
                        .mod(XEdDSA.ORDER), 256);
        return java.util.Arrays.equals(point, Rcheck);
    }

    /**
        Check if received Montgomery form key is on X25519 curve, point is
        found on Ed25519 curve and signature is a valid value.
     */
    private static Boolean isValidSignature(
            byte[] senderMontPublicKey, byte[] point, byte[] signature) {
        if (Util.byteArrayToBigInteger(senderMontPublicKey).compareTo(X25519.PRIME) >= 0) {
            System.err.println("Invalid sender public key.");
            return false;
        } else if (Util.byteArrayToBigInteger(point).compareTo(
                BigInteger.TWO.pow(255)) >= 0) {
            System.err.println("Invalid point in the encoded message.");
            return false;
        } else if (Util.byteArrayToBigInteger(signature).compareTo(
                BigInteger.TWO.pow(253)) >= 0) {
            System.err.println("Invalid signature.");
            return false;
        }
        return true;
    }
}
