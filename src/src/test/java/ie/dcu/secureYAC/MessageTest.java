package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

public class MessageTest {

    @Test
    public void headerExtractTest() throws Exception {
        User alice = new User("alice", 50);
        User bob = new User("bob", 50);
        Session test = new Session(alice, bob.getPreKeyBundle(), false);
        Message testMessage = test.DR.ratchetEncrypt("Hello World!".getBytes());
        testMessage.extractHeader();
        assertEquals(test.DR.getSendingMessageNo(), testMessage.getCurrentMessageNo());
        assertEquals(test.DR.getPrevSendingChainMessageNo(), testMessage.getPrevMessageNo());
    }

    @Test
    public void verifyTest() {
        byte[] plaintext = "Hello World!".getBytes();
        byte[] authKey = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(authKey);
        byte[] AD = Util.HMAC(plaintext, authKey);
        Message testMessage = new Message(null, AD, null);
        assertTrue(testMessage.verify(plaintext, authKey));
    }
    
}
