package ie.dcu.secureYAC;

public class User {
    private String username;
    private IdentityKeyBundle identity;

    User(IdentityKeyBundle identity) {
        this.username = identity.getUsername();
        this.identity = identity;
    }

    User(String username, int keyCount) throws Exception {
        this.username = username;
        this.identity = new IdentityKeyBundle(username,
            X25519.generatePrivateKey(), X25519.generatePrivateKey(), keyCount);
    }

    public String getUsername() { return this.username; }
    public IdentityKeyBundle getIdentityKeyBundle() { return this.identity; }
    
    public PreKeyBundle getPreKeyBundle()
        throws Exception {
        PreKeyBundle preKeyBundle = new PreKeyBundle(this, this.getIdentityKeyBundle());
        return preKeyBundle;
    }

    public static PreKeyBundle createInitPreKeyBundle(User user,
        Session session, PreKeyBundle preKeyBundle) throws Exception {
        return new PreKeyBundle(user.getUsername(),
            user.getIdentityKeyBundle().getIdentityPublicKey(),
            session.X3DH.getEphemeralPublicKey(),
            XEdDSA.sign(new XEdDSA(session.X3DH.getEphemeralPrivateKey())),
            preKeyBundle.getOneTimePreKey());

    }
}