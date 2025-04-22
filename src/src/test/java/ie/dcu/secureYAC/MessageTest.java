package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ie.dcu.secureYAC.Message.MessageType;

import java.security.SecureRandom;

public class MessageTest {
    @Test
    public void testHeaderExtract() throws Exception {
        User alice = new User("alice", 50);
        User bob = new User("bob", 50);
        Session test = new Session(alice, bob.getPreKeyBundle(), false);
        Message testMessage = test.DR.ratchetEncrypt(
                "Hello World!".getBytes(), MessageType.TEXT);
        testMessage.extractHeader();
        assertEquals(
                test.DR.getSendingMessageNo(), testMessage.getCurrentMessageNo());
        assertEquals(test.DR.getPrevSendingChainMessageNo(),
                testMessage.getPrevMessageNo());
    }

    @Test
    public void testVerify() {
        byte[] plaintext = "Hello World!".getBytes();
        byte[] authKey = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(authKey);
        byte[] AD = Util.HMAC(plaintext, authKey);
        Message testMessage = new Message(MessageType.TEXT, null, AD, null);
        assertTrue(testMessage.verify(plaintext, authKey));
    }

    @Test
    public void testBadVerify() {
        byte[] plaintext = "Hello World!".getBytes();
        byte[] authKey = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(authKey);
        byte[] AD = Util.HMAC(plaintext, authKey);
        Message testMessage = new Message(MessageType.TEXT, null, AD, null);
        random.nextBytes(authKey);
        assertFalse(testMessage.verify(plaintext, authKey));
    }
}
