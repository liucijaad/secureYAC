package ie.dcu.secureYAC;

import java.security.SecureRandom;
import java.util.Random;

/**
 *
    This class represents an open messaging session between users.
    Used for a high-level integration of cryptographic algorithms,
    storing user connection data, and exchange of
    {@link ie.dcu.secureYAC.Message Messages}.
 *
 * @author Liucija Paulina Adomaviciute
 */

public class Session {
    private int sessionID;
    private String sessionName;
    private User user;
    final X3DH X3DH;
    final DoubleRatchet DR;

    Session(User user, PreKeyBundle targetUser, Boolean receiving)
            throws Exception {
        Random random = new SecureRandom();
        this.sessionID = random.nextInt();
        this.sessionName = targetUser.getUsername();
        this.user = user;
        this.X3DH = new X3DH(this.user, targetUser, receiving);
        this.DR = new DoubleRatchet(this.user.getIdentityKeyBundle(), targetUser,
                X3DH.getSharedSecretValue());
    }

    public int getSessionID() {
        return this.sessionID;
    }

    public String getSessionName() {
        return this.sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public void sendMessage() {
    }

    public void receiveMessage() {
    }
}
