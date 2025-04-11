package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

public class IntegrationTest {
    @Test
    public void testSimulatedConversation() throws Exception {
        //Create users.
        User alice = new User("alice", 50);
        User bob = new User("bob", 50);
        //Bob exchanges prekey bundle with Alice.
        PreKeyBundle pkb = bob.getPreKeyBundle();
        Session aliceToBob = new Session(alice, pkb, false);
        //Alice sends her initialization bundle and the initial message.
        PreKeyBundle initPKB = User.createInitPreKeyBundle(alice, aliceToBob, pkb);
        Message message = aliceToBob.sendTextMessage("Hello, Bob.");
        //Bob receives the message.
        Session bobToAlice = new Session(bob, initPKB, true);
        String aliceMessageText = bobToAlice.receiveTextMessage(message);
        //Check if Bob correctly received Alice's message.
        //Generate a decoded "file".
        assertEquals("Hello, Bob.", aliceMessageText);
        Random random = new Random();
        byte[] decodedFile = new byte[random.nextInt(120)];
        random.nextBytes(decodedFile);
        //Bob send Alice the "file".
        message = bobToAlice.sendFile(decodedFile);
        //Alice receives the message with "file".
        byte[] aliceFileMessage = aliceToBob.receiveFileMessage(message);
        //Check if byte array Alice received is correct.
        assertArrayEquals(decodedFile, aliceFileMessage);
    }
}
