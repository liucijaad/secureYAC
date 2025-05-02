package ie.dcu.secureYAC;

import java.security.NoSuchAlgorithmException;

/**
 *
    This class represents the X3DH key agreement protocol.
    Implementation based on specification and references in
    <a href="https://signal.org/docs/specifications/x3dh/">
    The X3DH Key Agreement Protocol</a>.
 *
 * @author Liucija Paulina Adomaviciute
 */

public class X3DH {
    private IdentityKeyBundle identity;
    private PreKeyBundle preKeyBundle;
    private byte[] ephemeralPrivateKey;
    private byte[] ephemeralPublicKey;
    private byte[] sharedSecretValue;

    X3DH(User user, PreKeyBundle preKeyBundle, Boolean received)
            throws Exception {
        if (!XEdDSA.verify(preKeyBundle)) {
            throw new Exception("Invalid pre-key signature.");
        }
        this.identity = user.getIdentityKeyBundle();
        this.preKeyBundle = preKeyBundle;
        if (!received) {
            this.generateEphemeralKeys();
        } else {
            this.ephemeralPrivateKey = identity.getPreKeyPrivate();
            this.ephemeralPublicKey = identity.getPreKeyPublic();
        }
        this.calculateSharedSecret();
    }

    public byte[] getEphemeralPrivateKey() {
        return this.ephemeralPrivateKey;
    }

    public byte[] getEphemeralPublicKey() {
        return this.ephemeralPublicKey;
    }

    public byte[] getSharedSecretValue() {
        return this.sharedSecretValue;
    }

    private void generateEphemeralKeys() {
        this.ephemeralPrivateKey = X25519.generatePrivateKey();
        this.ephemeralPublicKey = X25519.generatePublicKey(this.ephemeralPrivateKey);
    }

    private static byte[] DH(byte[] keyOne, byte[] keyTwo) {
        return Util.bigIntToByteArray(Util.byteArrayToBigInteger(keyOne).multiply(
                Util.byteArrayToBigInteger(keyTwo)),
                512);
    }

    private byte[] HKDF(byte[] concatValues) throws NoSuchAlgorithmException {
        byte[] inputBytes = new byte[concatValues.length];
        for (int i = 0; i < 32; i++) {
            inputBytes[i] = (byte) 0xFF;
        }
        byte[] keyInputMaterial = Util.concatByteArrays(concatValues, inputBytes);
        return Util.HKDF(keyInputMaterial, null, "SecureYAC");
    }

    public void calculateSharedSecret() throws NoSuchAlgorithmException {
        byte[] firstValue = X3DH.DH(this.identity.getIdentityPublicKey(),
                this.preKeyBundle.getPreKeyPublic());
        byte[] secondValue = X3DH.DH(
                this.ephemeralPublicKey, this.preKeyBundle.getIdentityPublicKey());
        byte[] thirdValue = X3DH.DH(
                this.ephemeralPublicKey, this.preKeyBundle.getPreKeyPublic());
        byte[] fourthValue = null;
        byte[] concatValues = null;
        byte[] OTPK = this.preKeyBundle.getOneTimePreKey();
        if (identity.containsOTPK(OTPK)) {
            fourthValue = X3DH.DH(this.preKeyBundle.getPreKeyPublic(), OTPK);
            concatValues = Util.concatByteArrays(
                    Util.concatByteArrays(secondValue, firstValue),
                    Util.concatByteArrays(thirdValue, fourthValue));
        } else {
            fourthValue = X3DH.DH(this.ephemeralPublicKey, OTPK);
            concatValues = Util.concatByteArrays(
                    Util.concatByteArrays(firstValue, secondValue),
                    Util.concatByteArrays(thirdValue, fourthValue));
        }
        this.sharedSecretValue = this.HKDF(concatValues);
    }
}
