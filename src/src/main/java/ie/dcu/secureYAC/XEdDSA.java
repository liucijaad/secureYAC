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
 * @author Liucija Paulina Adomaviciute */

public class XEdDSA {
    private static final BigInteger PRIME =
    new BigInteger("7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFED", 16);
    private static final BigInteger BASE_POINT =
    new BigInteger("216936D3CD6E53FEC0A4E231FDD6DC5C692CC7609525A7B2C9562D608F25D51A", 16);
    private static final BigInteger ORDER =
    new BigInteger("1000000000000000000000000000000014DEF9DEA2F79CD65812631A5CF5D3ED", 16);

    private byte[] publicKey;
    private byte[] privateKey;

    XEdDSA(byte[] byteMontPrivateKey) {
        BigInteger montPrivateKey = Util.byteArrayToBigInteger(byteMontPrivateKey);
        this.publicKey = calculatePublicKey(montPrivateKey);
        this.privateKey = calculatePrivateKey(montPrivateKey);
    }

    private static byte[] calculatePublicKey(BigInteger montPrivateKey) {
        byte[] edPublicKey = Util.bigIntToByteArray(montPrivateKey
            .multiply(BASE_POINT).mod(ORDER), 256);
        //force sign bit to 0.
        byte first = edPublicKey[0];
        first &= ~(1 << 7);
        edPublicKey[0] = first;
        return edPublicKey;
    }

    private byte[] calculatePrivateKey(BigInteger montPrivateKey) {
        byte[] edPrivateKey = Util.bigIntToByteArray(montPrivateKey
            .abs().mod(ORDER), 256);
        return edPrivateKey;
    }

    public byte[] getPublicKey() { return this.publicKey; }
    public byte[] getPrivateKey() { return this.privateKey; }

    public static byte[] sign(XEdDSA xEdDSA) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] randomData = new byte[64];
        random.nextBytes(randomData);
        byte[] byteConcat = Util.concatByteArrays(xEdDSA.privateKey, randomData);
        byteConcat[0] = 1; //Force sign bit to 1 for hash1.
        byte[] privateHash = Util.bigIntToByteArray(Util.byteArrayToBigInteger(
            Util.hash(byteConcat)).mod(ORDER), 256);
        byte[] point = Util.bigIntToByteArray(Util.byteArrayToBigInteger(privateHash)
            .multiply(BASE_POINT).mod(ORDER), 256);
        //System.out.println("Point A: " + Util.byteArrayToString(point));
        //System.out.println("Public Key A: " + Util.byteArrayToString(xEdDSA.publicKey));
        byteConcat = Util.concatByteArrays(point, xEdDSA.publicKey);
        //System.out.println("ByteConcat A1: " + Util.byteArrayToString(byteConcat));
        byte[] hash = Util.bigIntToByteArray(Util.byteArrayToBigInteger(Util
            .hash(byteConcat)).mod(ORDER), 256);
        //System.out.println(Util.byteArrayToString(hash));
        //signature =  privateHash + (hash * privateKey) % ORDER
        byte[] signature = Util.bigIntToByteArray(Util.byteArrayToBigInteger(privateHash)
            .add(Util.byteArrayToBigInteger(hash).multiply(Util
            .byteArrayToBigInteger(xEdDSA.privateKey))).mod(ORDER), 256);
        //System.out.println("Signature A: " + Util.byteArrayToString(signature));
        return Util.concatByteArrays(point, signature);
    }


    public static Boolean verify(byte[] senderEdPublicKey, byte[] signedMessage)
    throws NoSuchAlgorithmException {
        byte[] point = java.util.Arrays.copyOfRange(signedMessage, 0, 32);
        //System.out.println("Point B: " + Util.byteArrayToString(point));
        byte[] signature = java.util.Arrays.copyOfRange(signedMessage, 32, 64);
        //System.out.println("Signature B: " + Util.byteArrayToString(signature));
        if(!XEdDSA.isValidSignature(senderEdPublicKey, point, signature)) {
            return false;
        }
        byte[] byteConcat = Util.concatByteArrays(point, senderEdPublicKey);
        //System.out.println("Public Key B: " + Util.byteArrayToString(senderEdPublicKey));
        //System.out.println("ByteConcat B1: " + Util.byteArrayToString(byteConcat));
        byte[] hash = Util.hash(Util.bigIntToByteArray(Util.byteArrayToBigInteger(
            byteConcat).mod(ORDER), 256));
        //System.out.println(Util.byteArrayToString(hash));
        //Rcheck = ((signature * BASE_POINT) - (hash * EdPublicKey)) % ORDER.
        byte[] Rcheck = Util.bigIntToByteArray(Util.byteArrayToBigInteger(signature)
            .multiply(BASE_POINT).subtract(Util.byteArrayToBigInteger(hash)
            .multiply(Util.byteArrayToBigInteger(senderEdPublicKey))).mod(ORDER), 256);
        if(signature.equals(Rcheck)) {
            return true;
        }
        return false;
    }

    private static Boolean isValidSignature(byte[] senderMontPublicKey, byte[] point,
        byte[] signature) {
        if(Util.byteArrayToBigInteger(senderMontPublicKey).compareTo(PRIME) >= 0) {
            System.err.println("Invalid sender public key.");
            return false;
        } else if(Util.byteArrayToBigInteger(point).compareTo(
                BigInteger.TWO.pow(255)) >= 0) {
            System.err.println("Invalid point in the encoded message.");
            return false;
        } else if(Util.byteArrayToBigInteger(signature).compareTo(
                BigInteger.TWO.pow(253)) >= 0) {
            System.err.println("Invalid signature.");
            return false;
        }
        //if not on_curve(A):
        //return false
        return true;
    }
    public static void main(String[] args) throws Exception {
        byte[] senderMontPrivKey = X25519.generatePrivateKey();
        XEdDSA xeddsaSender = new XEdDSA(senderMontPrivKey);
        byte[] signature = XEdDSA.sign(xeddsaSender);
        XEdDSA.verify(senderMontPrivKey, signature);
    }
}
