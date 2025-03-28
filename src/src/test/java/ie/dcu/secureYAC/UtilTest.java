package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class UtilTest {

    SecureRandom random = new SecureRandom();

    @RepeatedTest(value = 10)
    public void testChangeEndian() {
        byte[] testByte = new byte[16];
        random.nextBytes(testByte);
        byte[] transformedByte = Util.changeEndian(testByte);
        transformedByte = Util.changeEndian(transformedByte);
        assertArrayEquals(testByte, transformedByte);
    }

    @RepeatedTest(value = 10)
    public void testByteArrayToBigIntToByteArray() {
        byte[] byteArr = new byte[32];
        random.nextBytes(byteArr);
        BigInteger arrBigInt = Util.byteArrayToBigInteger(byteArr).abs();
        byte[] result = Util.bigIntToByteArray(arrBigInt, 256);
        assertEquals(arrBigInt.toString(2), Util.byteArrayToBigInteger(result).toString(2));
    }

    @Test
    public void testConcatByteArrays() {
        byte[] firstArray = new byte[32];
        byte[] secondArray = new byte[32];
        random.nextBytes(firstArray);
        random.nextBytes(secondArray);
        byte[] concat = Util.concatByteArrays(firstArray, secondArray);
        assertArrayEquals(firstArray, java.util.Arrays.copyOfRange(concat, 0, 32));
        assertArrayEquals(secondArray, java.util.Arrays.copyOfRange(concat, 32, 64));
    }

    @RepeatedTest(value = 10)
    public void testHash() throws NoSuchAlgorithmException {
        byte[] testArr = new byte[random.nextInt(32)];
        random.nextBytes(testArr);
        assertEquals(Util.hash(testArr).length, 64);
    }

    @Test
    public void testHKDF() throws NoSuchAlgorithmException {
        byte[] key = new byte[32];
        random.nextBytes(key);
        byte[] salt = new byte[32];
        for(int i = 0; i < 32; i++) {
            salt[i] = (byte) 0;
        }
        byte[] firstHKDF = Util.HKDF(key, salt, "test");
        byte[] secondHKDF = Util.HKDF(key, salt, "test");
        assertArrayEquals(firstHKDF, secondHKDF);
    }

    @Test
    public void testHMAC() {
        byte[] key = new byte[32];
        random.nextBytes(key);
        byte[] firstHMAC = Util.HMAC("test".getBytes(), key);
        byte[] secondHMAC = Util.HMAC("test".getBytes(), key);
        assertArrayEquals(firstHMAC, secondHMAC);
    }

    @RepeatedTest(value = 10)
    public void testLoadFromIdFile() throws Exception {
        User test = new User("test", 50);
        IdentityKeyBundle identity = test.getIdentityKeyBundle();
        identity.export();
        for(File f : new File(".").listFiles()) {
            String fileName = f.getName();
            if(fileName.endsWith(".id")) {
                IdentityKeyBundle loaded =
                    (IdentityKeyBundle) Util.loadFromFile(fileName);
                f.delete();
                assertEquals(identity.getUsername(), loaded.getUsername());
                assertArrayEquals(identity.getPreKeyPrivate(),
                    loaded.getPreKeyPrivate());
                assertArrayEquals(identity.getIdentityPrivateKey(),
                    loaded.getIdentityPrivateKey());
            }
        }
    }

    @RepeatedTest(value = 10)
    public void testLoadFromPkbFile() throws Exception {
        User test = new User("test", 50);
        PreKeyBundle preKey = test.getPreKeyBundle();
        preKey.export();
        for(File f : new File(".").listFiles()) {
            String fileName = f.getName();
            if(fileName.endsWith(".pkb")) {
                PreKeyBundle loaded = (PreKeyBundle) Util.loadFromFile(fileName);
                f.delete();
                assertEquals(preKey.getUsername(), loaded.getUsername());
                assertArrayEquals(preKey.getIdentityPublicKey(),
                    loaded.getIdentityPublicKey());
                assertArrayEquals(preKey.getPreKeyPublic(),
                    loaded.getPreKeyPublic());
                assertArrayEquals(preKey.getPreKeySignature(),
                    loaded.getPreKeySignature());
                assertArrayEquals(preKey.getOneTimePreKey(),
                    loaded.getOneTimePreKey());
            }
        }
    }
}
