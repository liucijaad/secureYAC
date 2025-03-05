package ie.dcu.secureYAC;

import java.util.Arrays;

import org.junit.jupiter.api.RepeatedTest;

public class X3DHTest {

    @RepeatedTest(value = 10)
    public void testSharedSecretCalculation() throws Exception {
        IdentityKeyBundle alice = new IdentityKeyBundle(
        X25519.generatePrivateKey(), X25519.generatePrivateKey());
        IdentityKeyBundle bob = new IdentityKeyBundle(
        X25519.generatePrivateKey(), X25519.generatePrivateKey());
        PreKeyBundle bobBundle = new PreKeyBundle(
        bob.getIdentityPublicKey(),bob.getPreKeyPublic(),
        bob.getPreKeySignature(), bob.useOTPK());
        X3DH X3DHAlice = new X3DH(alice, bobBundle);
        PreKeyBundle aliceBundle = new PreKeyBundle(
        alice.getIdentityPublicKey(), X3DHAlice.getEphemeralPublicKey(),
        XEdDSA.sign(new XEdDSA(X3DHAlice.getEphemeralPrivateKey())), bobBundle.getOneTimePreKey());
        byte[] aliceSecret = X3DHAlice.calculateSharedSecret();
        X3DH X3DHBob = new X3DH(bob, aliceBundle, aliceBundle.getOneTimePreKey());
        byte[] bobSecret = X3DHBob.calculateSharedSecret();
        assert(Arrays.equals(aliceSecret, bobSecret));
    }
    
}
