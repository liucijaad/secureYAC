package ie.dcu.secureYAC;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

/** 
 *
    This class represents a bundle of keys that are related to the identity
    of the user. Includes identity key, prekey, prekey signature, an array
    of unused one-time prekeys and an array of one-time prekeys that have
    been exported and pending use.
*
* @author Liucija Paulina Adomaviciute */

public class IdentityKeyBundle extends KeyBundle {
    //One-time prekeys that have not been exported into a key bundle.
    private ArrayList<byte[]> OTPKFresh = new ArrayList<byte[]>();
    //One-time prekeys that have been exported but not used to initialise connection.
    private ArrayList<byte[]> OTPKPending = new ArrayList<byte[]>();

    IdentityKeyBundle(byte[] identityPrivateKey, byte[] preKeyPrivate)
            throws Exception {
        super(identityPrivateKey, X25519.generatePublicKey(identityPrivateKey),
        preKeyPrivate, X25519.generatePublicKey(preKeyPrivate));
        for(int i = 50; i != 0; i--) {
            generateOneTimePreKey();
        }
    }

    IdentityKeyBundle(byte[] identityPrivateKey,  byte[] preKeyPrivate, int keyCount)
            throws Exception {
        super(identityPrivateKey, X25519.generatePublicKey(identityPrivateKey),
        preKeyPrivate, X25519.generatePublicKey(preKeyPrivate));
        for(int i = keyCount; i != 0; i--) {
            generateOneTimePreKey();
        }
    }

    //Getters.
    public ArrayList<byte[]> getOTPKFresh() { return this.OTPKFresh; }
    public ArrayList<byte[]> getOTPKPending() { return this.OTPKPending; }

    public void generateOneTimePreKey() {
        OTPKFresh.add(X25519.generatePrivateKey());
    }

    /**
     * 
        Takes first prekey from fresh one-time prekey array and moves it to
        pending-use one-time prekey array.
    *
    * @return byte[] of the first one-time prekey in the fresh key array. */
    public byte[] useOTPK() throws Exception {
        if(OTPKFresh.isEmpty()) {
            generateOneTimePreKey();
        }
        byte[] OTPKPrivate = OTPKFresh.removeFirst();
        //Add key to pending-use array to prevent re-use of the prekeys.
        this.OTPKPending.add(OTPKPrivate);
        return X25519.generatePublicKey(OTPKPrivate);
    }

    public Boolean containsOTPK(byte[] OTPKPublic) {
        for(byte[] key : this.OTPKPending) {
            if(Arrays.equals(X25519.generatePublicKey(key), OTPKPublic)) {
                return true;
            }
        }
        return false;
    }

    public void removeOTPK(byte[] OTPKPublic) {
        byte[] foundKey = null;
        for(byte[] key : this.OTPKPending) {
            if(Arrays.equals(X25519.generatePublicKey(key), OTPKPublic))  {
                foundKey = key;
            }
        }
        this.OTPKPending.remove(foundKey);
    }

    public PreKeyBundle exportPreKeyBundle() throws Exception {
        byte[] otpk = this.useOTPK();
        return new PreKeyBundle(this.getIdentityPublicKey(), this.getPreKeyPublic(), this.getPreKeySignature(), otpk);
    }

    public void export() throws NoSuchAlgorithmException, IOException {
        String fileName = Util.byteArrayToBigInteger(Util.hash(this.getIdentityPublicKey())).toString(16).substring(1, 11);
        String data = Util.byteArrayToString(this.getIdentityPrivateKey()) + "\n" + Util.byteArrayToString(this.getPreKeyPrivate()) + "\n";
        Util.writeToFile(fileName, ".id", data, 0);
    }
}