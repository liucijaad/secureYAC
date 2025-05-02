package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.RepeatedTest;

public class XEdDSATest {
    @RepeatedTest(value = 10)
    public void testKeyLength() {
        byte[] montPrivateKey = X25519.generatePrivateKey();
        XEdDSA xeddsa = new XEdDSA(montPrivateKey);
        xeddsa.calculateKeyPair();
        assertEquals(32, xeddsa.getPublicKey().length);
    }

    @RepeatedTest(value = 10)
    public void testKeySignBit() {
        byte[] montPrivateKey = X25519.generatePrivateKey();
        XEdDSA xeddsa = new XEdDSA(montPrivateKey);
        xeddsa.calculateKeyPair();
        byte[] privateKey = xeddsa.getPrivateKey();
        assertEquals(0, privateKey[31] >>> 8 & 1);
    }

    @RepeatedTest(value = 10)
    public void testSignatureLength() throws Exception {
        User test = new User("test", 50);
        byte[] signature = XEdDSA.sign(test.getIdentityKeyBundle()
                .getIdentityPrivateKey(),
            X25519.generatePrivateKey());
        assertEquals(96, signature.length);
    }

    @RepeatedTest(value = 10)
    public void testSignAndVerify() throws Exception {
        User test = new User("test", 50);
        assertTrue(XEdDSA.verify(test.getPreKeyBundle()));
    }

    @RepeatedTest(value = 10)
    public void testBadSignAndVerify() throws Exception {
        User test = new User("test", 50);
        PreKeyBundle pkb = test.getPreKeyBundle();
        pkb.preKeySignature[0] = (byte) 0x00; 
        assertFalse(XEdDSA.verify(pkb));
    }
}