package ie.dcu.secureYAC;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/**
 *
    This class contains a collection of utility methods.
 *
 * @author Liucija Paulina Adomaviciute
 */

public final class Util {
    private Util() {
    }

    /**
        Change byte array order from bit-endian to little-endian
        and vice versa.
     */
    public static byte[] changeEndian(byte[] array) {
        byte[] changedEndian = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            changedEndian[i] = array[array.length - 1 - i];
        }
        return changedEndian;
    }

    /**
        Convert BigInteger into a byte array.
     *
     * @return byte array representation of BigInteger in little-endian.
     */
    public static byte[] bigIntToByteArray(BigInteger bigInteger, int expectedBits) {
        String binary = bigInteger.toString(2);
        while (binary.length() < expectedBits || binary.length() % 8 != 0) {
            binary = "0" + binary;
        }
        byte[] array = new byte[binary.length() / 8];
        int arrPos = 0;
        for (int i = 0; i < binary.length(); i += 8) {
            String substring = binary.substring(i, i + 8);
            Integer numeric = Integer.parseInt(substring, 2);
            array[arrPos] = numeric.byteValue();
            arrPos++;
        }
        return Util.changeEndian(array);
    }

    /** @return hexadecimal String of byte array. */
    public static String byteArrayToString(byte[] byteArray) {
        return new BigInteger(Util.changeEndian(byteArray)).toString(16);
    }

    /** Convert little-endian byte array into BigInteger. */
    public static BigInteger byteArrayToBigInteger(byte[] byteArray) {
        return new BigInteger(Util.changeEndian(byteArray));
    }

    /**
        Concatenates two byte arrays A and B in little-endian format.
     *
     * @return byte array in format A || B in little-endian.
     */
    public static byte[] concatByteArrays(byte[] A, byte[] B) {
        byte[] concat = new byte[A.length + B.length];
        System.arraycopy(A, 0, concat, 0, A.length);
        System.arraycopy(B, 0, concat, A.length, B.length);
        return concat;
    }

    /** Hash byte array using SHA-512. */
    public static byte[] hash(byte[] data) throws NoSuchAlgorithmException {
        String hash = "";
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] messageDigest = md.digest(data);
        hash = new BigInteger(1, messageDigest).toString(2);
        // Pad the hash if it is shorter than 512 bits.
        while (hash.length() < 512) {
            hash = "0" + hash;
        }
        return Util.bigIntToByteArray(new BigInteger(hash, 2), 512);
    }

    /**
        Derive new keys using HKDF with SHA-512 hash function.
     *
     * @throws NoSuchAlgorithmException
     */
    public static byte[] HKDF(byte[] keyInputMaterial, byte[] salt, String info)
            throws NoSuchAlgorithmException {
        HKDFParameters param = new HKDFParameters(keyInputMaterial, salt, info.getBytes());
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(param);
        byte[] hkdfKey = new byte[64];
        hkdf.generateBytes(hkdfKey, 0, 64);
        return hkdfKey;
    }

    /** HMAC with SHA-256 hash function. */
    public static byte[] HMAC(byte[] input, byte[] key) {
        HMac hMac = new HMac(new SHA256Digest());
        hMac.init(new KeyParameter(key));
        hMac.update(input, 0, input.length);
        byte[] hMacOutput = new byte[32];
        hMac.doFinal(hMacOutput, 0);
        return hMacOutput;
    }

    public static KeyBundle loadFromFile(String fileName) throws Exception {
        KeyBundle keyBundle = null;
        File file = new File(fileName);
        Scanner reader = new Scanner(file);
        String username = reader.nextLine();
        byte[] firstKey = Util.bigIntToByteArray(new BigInteger(reader.nextLine(), 16), 256);
        byte[] secondKey = Util.bigIntToByteArray(new BigInteger(reader.nextLine(), 16), 256);
        if (fileName.contains(".id")) {
            keyBundle = new IdentityKeyBundle(username, firstKey, secondKey, 50);
        } else if (fileName.contains(".pkb")) {
            byte[] preKeySignature = Util.bigIntToByteArray(
                    new BigInteger(reader.nextLine(), 16), 768);
            byte[] otpk = Util.bigIntToByteArray(
                    new BigInteger(reader.nextLine(), 16), 256);
            keyBundle = new PreKeyBundle(
                    username, firstKey, secondKey, preKeySignature, otpk);
        }
        reader.close();
        return keyBundle;
    }

    public static int writeToFile(String fileName, String fileType, String data,
            int fileCount) throws IOException {
        String fileFullName = fileName + fileType;
        if (fileCount != 0) {
            fileFullName = fileName + "_" + (fileCount + 1) + fileType;
        }
        File file = new File(fileFullName);
        if (!file.createNewFile()) {
            return Util.writeToFile(fileName, fileType, data, fileCount += 1);
        }
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        writer.write(data);
        writer.close();
        return 0;
    }
}
