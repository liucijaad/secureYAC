package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.RepeatedTest;

public class X25519Test {
    @RepeatedTest(value = 10)
    public void testKeyLength() {
        byte[] privateKey = X25519.generatePrivateKey();
        assertEquals(privateKey.length, 32);
        byte[] publicKey = X25519.generatePublicKey(privateKey);
        assertEquals(publicKey.length, 32);
    }

    @RepeatedTest(value = 10)
    public void testClamping() {
        byte[] key = X25519.generatePrivateKey();
        byte tmp;
        for (int k = 0; k < 3; k++) {
            tmp = key[0];
            assertEquals(0, (tmp >> 8 - k) & 1);
        }
        tmp = key[31];
        assertEquals(1, (tmp >> 1) & 1);
        assertEquals(0, tmp & 1);
    }

    @RepeatedTest(value = 10)
    public void testPublicKeyGeneration() {
        byte[] privateKey = X25519.generatePrivateKey();
        byte[] pubKeyOne = X25519.generatePublicKey(privateKey);
        byte[] pubKeyTwo = X25519.generatePublicKey(privateKey);
        assertArrayEquals(pubKeyOne, pubKeyTwo);
    }
}
