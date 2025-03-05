package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.jupiter.api.RepeatedTest;

public class UtilTest {

    @RepeatedTest(value = 100)
    public void testByteArrayToBigIntToByteArray() {
        byte[] byteArr = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(byteArr);
        BigInteger arrBigInt = Util.byteArrayToBigInteger(byteArr).abs();
        byte[] result = Util.bigIntToByteArray(arrBigInt, 256);
        assert(arrBigInt.toString(2).equals(Util.byteArrayToBigInteger(result).toString(2)));
    }

    @RepeatedTest(value = 100)
    public void testHash() throws NoSuchAlgorithmException {
        SecureRandom random = new SecureRandom();
        byte[] testArr = new byte[random.nextInt(32)];
        random.nextBytes(testArr);
        assertEquals(Util.hash(testArr).length, 64);
    }

    @RepeatedTest(value = 10)
    public void testLoadFromIdFile() throws Exception {
        IdentityKeyBundle identity = new IdentityKeyBundle(
        X25519.generatePrivateKey(), X25519.generatePrivateKey());
        identity.export();
        for(File f : new File(".").listFiles()) {
            String fileName = f.getName();
            if(fileName.endsWith(".id")) {
                IdentityKeyBundle loaded = (IdentityKeyBundle) Util.loadFromFile(fileName);
                System.out.println(Util.byteArrayToString(identity.getIdentityPrivateKey()));
                System.out.println(Util.byteArrayToString(loaded.getIdentityPrivateKey()));
                assertEquals(Util.byteArrayToString(identity.getIdentityPrivateKey()),
                Util.byteArrayToString(loaded.getIdentityPrivateKey()));
                f.delete();
            }
        }
    }
}
