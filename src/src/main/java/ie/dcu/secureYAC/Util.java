package ie.dcu.secureYAC;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/** 
 * 
    This class contains methods used for utility in multiple classes.
 *
 * @author Liucija Paulina Adomaviciute */

public final class Util {

    private Util() {}

    public static byte[] changeEndian(byte[] array) {
        byte[] changedEndian = new byte[array.length];
        for(int i = 0; i < array.length; i++) {
            changedEndian[i] = array[array.length - 1 - i];
        }
        return changedEndian;
    }

    public static byte[] bigIntToByteArray(BigInteger bigInteger, int expectedBits) {
        String binary = bigInteger.toString(2);
        while(binary.length() < expectedBits || binary.length() % 8 != 0) {
            binary =  "0" + binary;
        }
        byte[] array = new byte[binary.length() / 8];
        int arrPos = 0;
        for(int i = 0; i < binary.length(); i += 8) {
            String substring = binary.substring(i, i + 8);
            Integer numeric = Integer.parseInt(substring, 2); 
            array[arrPos] = numeric.byteValue();
            arrPos ++;
        }
        return Util.changeEndian(array);
    }

    public static String byteArrayToString(byte[] byteArray) {
        return new BigInteger(Util.changeEndian(byteArray)).toString(16);
    }

    public static BigInteger byteArrayToBigInteger(byte[] byteArray) {
        return new BigInteger(Util.changeEndian(byteArray));
    }

    public static byte[] hash(byte[] message) throws NoSuchAlgorithmException {
        String hash = "";
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] messageDigest = md.digest(message);
        hash = new BigInteger(1, messageDigest).toString(2);
        //Pad the hash if it is shorter than 512 bits.
        while(hash.length() < 512) {
            hash = "0" + hash;
        }
        return Util.bigIntToByteArray(new BigInteger(hash, 2), 512);
    }

    public static byte[] concatByteArrays(byte[] firstArr, byte[] secondArr) {
        byte[] concat = new byte[firstArr.length + secondArr.length];
        System.arraycopy(firstArr, 0, concat, 0, firstArr.length);
        System.arraycopy(secondArr, 0, concat, firstArr.length, secondArr.length);
        return concat;
    }

    public static KeyBundle loadFromFile(String fileName) throws Exception {
        KeyBundle keyBundle = null;
        File file = new File(fileName);
        Scanner reader = new Scanner(file);
        byte[] firstKey = Util.bigIntToByteArray(new BigInteger(reader.nextLine(), 16), 256);
        byte[] secondKey = Util.bigIntToByteArray(new BigInteger(reader.nextLine(), 16), 256);
        if(fileName.contains(".id")) {
            keyBundle = new IdentityKeyBundle(firstKey, secondKey);
        }
        else if(fileName.contains(".pkb")) {
            byte[] preKeySignature = Util.bigIntToByteArray(new BigInteger(reader.nextLine(), 16), 512);
            byte[] otpk = Util.bigIntToByteArray(new BigInteger(reader.nextLine(), 16), 256);
            keyBundle = new PreKeyBundle(firstKey, secondKey, preKeySignature, otpk);
        }
        reader.close();
        return keyBundle;
    }

    public static int writeToFile(String fileName, String fileType, String data, int fileCount) throws IOException {
        String fileFullName = fileName + fileType;
        if(fileCount != 0) {
            fileFullName = fileName + "_" + (fileCount + 1) + fileType;
        }
        File file = new File(fileFullName);
        if(!file.createNewFile()) {
            return Util.writeToFile(fileName, fileType, data, fileCount+= 1);
        }
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        writer.write(data);
        writer.close();
        return 0;
    }
}
