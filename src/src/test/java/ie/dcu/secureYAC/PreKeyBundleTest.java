package ie.dcu.secureYAC;

import java.io.File;

import org.junit.jupiter.api.RepeatedTest;

public class PreKeyBundleTest {
    
    @RepeatedTest(value = 10)
    public void testExport() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle(
        X25519.generatePrivateKey(), X25519.generatePrivateKey());
        PreKeyBundle prekey = new PreKeyBundle(identity.getIdentityPublicKey(),
        identity.getPreKeyPublic(), identity.getPreKeySignature(), identity.useOTPK());
        for(int i = 10; i !=0; i--) {
            prekey.export();
        }
        for(File f : new File(".").listFiles()) {
            if(f.getName().endsWith(".pkb")) {
                f.delete();
            }
        }
    }
}
