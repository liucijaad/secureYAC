package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

public class IdentityKeyBundleTest {
    
    @Test
    public void TestOTPKInitNoKeyCount() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle(
        X25519.generatePrivateKey(), X25519.generatePrivateKey());
        assertEquals(identity.getOTPKFresh().size(), 50);
        assertEquals(identity.getOTPKPending().size(), 0);
    }

    @Test
    public void testOTPKInit() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle(
        X25519.generatePrivateKey(), X25519.generatePrivateKey(), 150);
        assertEquals(identity.getOTPKFresh().size(), 150);
        assertEquals(identity.getOTPKPending().size(), 0);
    }

    @Test
    public void testOTPKGeneration() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle(
        X25519.generatePrivateKey(), X25519.generatePrivateKey());
        assertEquals(identity.getOTPKFresh().size(), 50);
        identity.generateOneTimePreKey();
        assertEquals(identity.getOTPKFresh().size(), 51);
    }
    @Test
    public void testUseOTPK() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle(
        X25519.generatePrivateKey(), X25519.generatePrivateKey());
        byte[] usedKey = identity.useOTPK();
        assertEquals(identity.getOTPKFresh().size(), 49);
        assertEquals(identity.getOTPKPending().size(), 1);
        assert(identity.containsOTPK(usedKey));
        identity.removeOTPK(usedKey);
        assertEquals(identity.getOTPKPending().size(), 0);
    }

    @Test
    public void testExport() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle(
        X25519.generatePrivateKey(), X25519.generatePrivateKey());
        for(int i = 10; i !=0; i--) {
            identity.export();
        }
        for(File f : new File(".").listFiles()) {
            if(f.getName().endsWith(".id")) {
                f.delete();
            }
        }
    }
}
