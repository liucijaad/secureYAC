package ie.dcu.secureYAC;

/** @author Liucija Paulina Adomaviciute */

public class KeyBundle {
    private byte[] identityPublicKey;
    private byte[] identityPrivateKey;
    private byte[] preKeyPublic;
    private byte[] preKeyPrivate;
    private byte[] preKeySignature;

    KeyBundle(byte[] identityPublicKey, byte[] preKeyPublic) {
        this.identityPublicKey = identityPublicKey;
        this.preKeyPublic = preKeyPublic;
    }
    KeyBundle(byte[] identityPublicKey, byte[] preKeyPublic, byte[] preKeySignature) {
        this.identityPublicKey = identityPublicKey;
        this.preKeyPublic = preKeyPublic;
        this.preKeySignature = preKeySignature;
    }

    KeyBundle(byte[] identityPrivateKey, byte[] identityPublicKey,
    byte[] preKeyPrivate, byte[] preKeyPublic) throws Exception {
        this.identityPrivateKey = identityPrivateKey;
        this.preKeyPrivate = preKeyPrivate;
        this.identityPublicKey = identityPublicKey;
        this.preKeyPublic = preKeyPublic;
        this.preKeySignature = XEdDSA.sign(new XEdDSA(identityPrivateKey));
    }

    public byte[] getIdentityPublicKey() { return this.identityPublicKey; }
    public byte[] getIdentityPrivateKey() { return this.identityPrivateKey; }
    public byte[] getPreKeyPublic() { return this.preKeyPublic; }
    public byte[] getPreKeyPrivate() { return this.preKeyPrivate; }
    public byte[] getPreKeySignature() { return this.preKeySignature; }
    public byte[] getOneTimePreKey() { return this.getPreKeyPublic(); }
}