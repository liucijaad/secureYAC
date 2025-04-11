package ie.dcu.secureYAC;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 *
    This class represents a bundle of prekeys that are exported and given to
    another user to initialise conversation between the owner and the receiver
    of the pre-key bundle.
 *
 * @author Liucija Paulina Adomaviciute
 */

public class PreKeyBundle extends KeyBundle {
    private byte[] oneTimePreKey;

    PreKeyBundle(String username, byte[] identityPublicKey, byte[] preKeyPublic,
            byte[] preKeySignature, byte[] oneTimePreKey) {
        super(username, identityPublicKey, preKeyPublic, preKeySignature);
        this.oneTimePreKey = oneTimePreKey;
    }

    PreKeyBundle(User user, IdentityKeyBundle identityKeyBundle) throws Exception {
        super(user.getUsername(), identityKeyBundle.getIdentityPublicKey(),
                identityKeyBundle.getPreKeyPublic(),
                identityKeyBundle.getPreKeySignature());
        this.oneTimePreKey = identityKeyBundle.useOTPK();
    }

    @Override
    public byte[] getOneTimePreKey() {
        return this.oneTimePreKey;
    }

    public void export() throws NoSuchAlgorithmException, IOException {
        String fileName = Util.byteArrayToBigInteger(Util.hash(oneTimePreKey))
                .toString(16)
                .substring(1, 11);
        String data = this.getUsername() + "\n"
                + Util.byteArrayToString(this.getIdentityPublicKey()) + "\n"
                + Util.byteArrayToString(this.getPreKeyPublic()) + "\n"
                + Util.byteArrayToString(this.getPreKeySignature()) + "\n"
                + Util.byteArrayToString(this.getOneTimePreKey());
        Util.writeToFile(fileName, ".pkb", data, 0);
    }
}