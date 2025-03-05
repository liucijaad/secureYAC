package ie.dcu.secureYAC;

import java.security.NoSuchAlgorithmException;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

/**
*
    This class represents the X3DH key agreement protocol.
    Implementation based on specification and references in
    <a href="https://signal.org/docs/specifications/x3dh/">
    The X3DH Key Agreement Protocol</a>.
*
* @author Liucija Paulina Adomaviciute */

public class X3DH {

    private IdentityKeyBundle identity;
    private PreKeyBundle receivedIdentity;
    private byte[] ephemeralPublicKey;
    private byte[] ephemeralPrivateKey;

    X3DH(IdentityKeyBundle identity) {
        this.identity = identity;
        this.generateEphemeralKey();
    }

    X3DH(IdentityKeyBundle identity, PreKeyBundle receivedIdentity) {
        this.identity = identity;
        this.receivedIdentity = receivedIdentity;
        this.generateEphemeralKey();
    }

    X3DH(IdentityKeyBundle identity, PreKeyBundle receivedIdentity, byte[] OTPK) {
        this.identity = identity;
        this.receivedIdentity = receivedIdentity;
        this.ephemeralPublicKey = identity.getPreKeyPublic();
    }

    //Getters
    public IdentityKeyBundle getIdentity() { return this.identity; }
    public KeyBundle getReceivedIdentity() { return this.receivedIdentity; }
    public byte[] getEphemeralPublicKey() { return this.ephemeralPublicKey; }
    public byte[] getEphemeralPrivateKey() { return this.ephemeralPrivateKey; }

    //Setters
    public void setReceivedIdentity(PreKeyBundle receivedIdentity) {
        this.receivedIdentity = receivedIdentity;
    }

    private void generateEphemeralKey() {
        this.ephemeralPrivateKey = X25519.generatePrivateKey();
        this.ephemeralPublicKey = X25519.generatePublicKey(this.ephemeralPrivateKey);
    }

    private static byte[] HKDF(byte[] byteValue) throws NoSuchAlgorithmException {
        byte[] inputBytes = new byte[byteValue.length];
        for(int i = 0; i < 32; i++) {
            inputBytes[i] = (byte) 0xFF;
        }
        byte[] keyInputMaterial = Util.concatByteArrays(inputBytes, byteValue);
        HKDFParameters param = new HKDFParameters(keyInputMaterial, null, "SecureYAC".getBytes());
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(param);
        byte[] hkdfKey = new byte[32];
        hkdf.generateBytes(hkdfKey, 0, 32);
        return hkdfKey;
    }

    private static byte[] DH(byte[] keyOne, byte[] keyTwo) {
        return Util.bigIntToByteArray(Util.byteArrayToBigInteger(keyOne)
        .multiply(Util.byteArrayToBigInteger(keyTwo)), 512);
    }

    public byte[] calculateSharedSecret() throws NoSuchAlgorithmException {
        byte[] receivedIdentityKey = receivedIdentity.getIdentityPublicKey();
        byte[] receivedSignedPreKey = receivedIdentity.getPreKeyPublic();
        byte[] firstValue = X3DH.DH(this.identity.getIdentityPublicKey(), receivedSignedPreKey);
        byte[] secondValue = X3DH.DH(this.ephemeralPublicKey, receivedIdentityKey);
        byte[] thirdValue = X3DH.DH(this.ephemeralPublicKey, receivedSignedPreKey);
        byte[] fourthValue = null;
        byte[] concatValues = null;
        if(identity.containsOTPK(this.receivedIdentity.getOneTimePreKey())) {
            fourthValue = X3DH.DH(this.receivedIdentity.getPreKeyPublic(), this.receivedIdentity.getOneTimePreKey());
            concatValues = Util.concatByteArrays(Util.concatByteArrays(thirdValue, fourthValue),
            Util.concatByteArrays(firstValue, secondValue));
        }
        else {
            fourthValue = X3DH.DH(this.ephemeralPublicKey, this.receivedIdentity.getOneTimePreKey());
            concatValues = Util.concatByteArrays(Util.concatByteArrays(thirdValue, fourthValue),
            Util.concatByteArrays(secondValue, firstValue));
        }
        byte[] finalValue = X3DH.HKDF(concatValues);
        return finalValue;
    }
}

