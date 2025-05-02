package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.RepeatedTest;

public class X3DHTest {
    @RepeatedTest(value = 10)
    public void testSharedSecretCalculation() throws Exception {
        User alice = new User("Alice", 50);
        User bob = new User("Bob", 50);
        PreKeyBundle bobBundle = bob.getPreKeyBundle();
        Session aliceSession = new Session(alice, bobBundle, false);
        PreKeyBundle aliceBundle = User.createInitPreKeyBundle(alice, aliceSession, bobBundle);
        Session bobSession = new Session(bob, aliceBundle, true);
        assertArrayEquals(aliceSession.X3DH.getSharedSecretValue(),
                bobSession.X3DH.getSharedSecretValue());
    }
}
