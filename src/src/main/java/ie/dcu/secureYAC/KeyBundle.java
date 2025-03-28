package ie.dcu.secureYAC;

/** @author Liucija Paulina Adomaviciute */

public class KeyBundle {
    private String username;
    private byte[] identityPublicKey;
    private byte[] identityPrivateKey;
    private byte[] preKeyPublic;
    private byte[] preKeyPrivate;
    private byte[] preKeySignature;

    KeyBundle(String username, byte[] identityPublicKey, byte[] preKeyPublic) {
        this.username = username;
        this.identityPublicKey = identityPublicKey;
        this.preKeyPublic = preKeyPublic;
    }
    KeyBundle(String username, byte[] identityPublicKey,
        byte[] preKeyPublic, byte[] preKeySignature) {
        this.username = username;
        this.identityPublicKey = identityPublicKey;
        this.preKeyPublic = preKeyPublic;
        this.preKeySignature = preKeySignature;
    }

    KeyBundle(String username, byte[] identityPrivateKey, byte[] identityPublicKey,
    byte[] preKeyPrivate, byte[] preKeyPublic) throws Exception {
        this.username = username;
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
    public String getUsername() { return this.username; }

}