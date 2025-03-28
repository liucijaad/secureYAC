package ie.dcu.secureYAC;

import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;

import org.junit.jupiter.api.Test;

public class DoubleRatchetTest {

    @Test
    public void testEncryptAndDecrypt() throws Exception {
        Random random = new SecureRandom();
        byte[] plaintext = new byte[60];
        random.nextBytes(plaintext);
        User alice = new User("Alice", 50);
        User bob = new User("Bob", 50);
        PreKeyBundle bobPreKey = bob.getPreKeyBundle();
        Session aliceSession = new Session(alice, bobPreKey, false);
        Message message = aliceSession.DR.ratchetEncrypt(plaintext);
        PreKeyBundle aliceBundle = User.createInitPreKeyBundle(alice, aliceSession, bobPreKey);
        Session bobSession = new Session(bob, aliceBundle, true);
        byte[] decrypted = bobSession.DR.ratchetDecrypt(message);
        assert(java.util.Arrays.equals(plaintext, decrypted));
    }

    @Test
    public void testAES() throws Exception {
        User alice = new User("Alice", 50);
        User bob = new User("Bob", 50);
        PreKeyBundle bobPreKey = bob.getPreKeyBundle();
        Session aliceSession = new Session(alice, bobPreKey, false);
        Random random = new SecureRandom();
        byte[] key = new byte[32];
        random.nextBytes(key);
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        byte[] plaintext = new byte[140];
        random.nextBytes(plaintext);
        byte[] ciphertext = aliceSession.DR.AES(Cipher.ENCRYPT_MODE, key, iv, plaintext);
        byte[] decrypted = aliceSession.DR.AES(Cipher.DECRYPT_MODE, key, iv, ciphertext);
        assert(java.util.Arrays.equals(plaintext, decrypted));
    }
}
