package ie.dcu.secureYAC;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class Session {

    private int sessionID;
    private String sessionName;
    private User user;
    X3DH X3DH;
    DoubleRatchet DR;

    public Session(User user, PreKeyBundle targetUser, Boolean receiving)
        throws NoSuchAlgorithmException {
        Random random = new SecureRandom();
        this.sessionID = random.nextInt();
        this.sessionName = targetUser.getUsername();
        this.user = user;
        this.X3DH = new X3DH(this.user, targetUser, receiving);
        this.DR = new DoubleRatchet(this.user.getIdentityKeyBundle(), targetUser, X3DH.getSharedSecretValue());
    }

    public int getSessionID() { return this.sessionID; }
    public String getSessionName() { return this.sessionName; }

    public void setSessionName(String sessionName) { this.sessionName = sessionName; }

    public void sendMessage() {}
    
    public void receiveMessage() {}
}
