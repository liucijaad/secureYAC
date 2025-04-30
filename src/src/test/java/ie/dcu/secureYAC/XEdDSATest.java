package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.RepeatedTest;

public class XEdDSATest {
    
    //@RepeatedTest(value = 10)
    //public void testKeyLength() {
    //    byte[] montPrivateKey = X25519.generatePrivateKey();
    //    XEdDSA xeddsa = new XEdDSA(montPrivateKey);
    //    assertEquals(xeddsa.getPublicKey().length, 32);
    //}
    //@RepeatedTest(value = 10)
    //public void testKeySignBit() {
    //    byte[] montPrivateKey = X25519.generatePrivateKey();
    //    XEdDSA xeddsa = new XEdDSA(montPrivateKey);
    //    byte[] privateKey = xeddsa.getPrivateKey();
    //    byte first = privateKey[0];
    //    assertEquals(first >>> 8 & 1, 0);
    //}
    //@RepeatedTest(value = 10)
    //public void testSignatureLength() throws Exception {
    //    byte[] montPrivateKey = X25519.generatePrivateKey();
    //    byte[] signature = XEdDSA.sign(new XEdDSA(montPrivateKey));
    //    assertEquals(signature.length, 64);
    //}

    //@RepeatedTest(value = 10)
    //public void testSignAndVerify() throws Exception {
    //    //sender signed key
    //    byte[] senderMontPrivKey = X25519.generatePrivateKey();
    //    XEdDSA xeddsaSender = new XEdDSA(senderMontPrivKey);
    //    byte[] signature = XEdDSA.sign(xeddsaSender);
    //    //receiver verification
    //    XEdDSA xeddsaReceiver = new XEdDSA(X25519.generatePrivateKey());
    //    assert(XEdDSA.verify(senderMontPrivKey, signature, xeddsaReceiver));
    //
    //}
}
