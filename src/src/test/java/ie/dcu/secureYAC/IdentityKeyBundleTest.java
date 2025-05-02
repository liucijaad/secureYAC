package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import java.io.File;

public class IdentityKeyBundleTest {
    @Test
    public void testInitOTPK() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle("test",
                X25519.generatePrivateKey(), X25519.generatePrivateKey(), 150);
        assertEquals(identity.getOTPKFresh().size(), 150);
        assertEquals(identity.getOTPKPending().size(), 0);
    }

    @Test
    public void testGenerateOTPK() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle("test",
                X25519.generatePrivateKey(), X25519.generatePrivateKey(), 50);
        assertEquals(identity.getOTPKFresh().size(), 50);
        identity.generateOneTimePreKey();
        assertEquals(identity.getOTPKFresh().size(), 51);
    }

    @Test
    public void testUseOTPK() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle("test",
                X25519.generatePrivateKey(), X25519.generatePrivateKey(), 50);
        byte[] usedKey = identity.useOTPK();
        assertEquals(identity.getOTPKFresh().size(), 49);
        assertEquals(identity.getOTPKPending().size(), 1);
        assertTrue(identity.containsOTPK(usedKey));
        identity.removeOTPK(usedKey);
        assertEquals(identity.getOTPKPending().size(), 0);
    }

    @Test
    public void testExport() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle("test",
                X25519.generatePrivateKey(), X25519.generatePrivateKey(), 50);
        identity.export();
        for (File f : new File(".").listFiles()) {
            if (f.getName().endsWith(".id")) {
                f.delete();
                return;
            }
        }
        fail(".id file not found.");
    }

    @Test
    public void testMultiExport() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle("test",
                X25519.generatePrivateKey(), X25519.generatePrivateKey(), 50);
        for (int i = 10; i != 0; i--) {
            identity.export();
        }
        int counter = 0;
        for (File f : new File(".").listFiles()) {
            if (f.getName().endsWith(".id")) {
                counter += 1;
                f.delete();
            }
        }
        assertEquals(10, counter);
    }
}
